package com.openclaw.assistant.ui.voice

import android.app.Application
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.data.SettingsRepository
import com.openclaw.assistant.speech.SpeechRecognizerManager
import com.openclaw.assistant.speech.SpeechResult
import com.openclaw.assistant.speech.TTSManager
import com.openclaw.assistant.speech.TTSState
import com.openclaw.assistant.speech.TTSUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "VoiceViewModel"

data class VoiceTurn(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false
)

class VoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = SettingsRepository.getInstance(application)
    private val nodeRuntime = (application as OpenClawApplication).nodeRuntime
    private val speechManager = SpeechRecognizerManager(application)
    private val ttsManager = TTSManager(application)

    private val _micLiveTranscript = MutableStateFlow<String?>(null)
    val micLiveTranscript = _micLiveTranscript.asStateFlow()

    private val _micInputLevel = MutableStateFlow(0f)
    val micInputLevel = _micInputLevel.asStateFlow()

    private val _micIsSending = MutableStateFlow(false)
    val micIsSending = _micIsSending.asStateFlow()

    private val _speakerEnabled = MutableStateFlow(settings.ttsEnabled)
    val speakerEnabled = _speakerEnabled.asStateFlow()

    private val _micEnabled = MutableStateFlow(false)
    val micEnabled = _micEnabled.asStateFlow()

    private val _micCooldown = MutableStateFlow(false)
    val micCooldown = _micCooldown.asStateFlow()

    private val _micStatusText = MutableStateFlow("")
    val micStatusText = _micStatusText.asStateFlow()

    private val _micConversation = MutableStateFlow<List<VoiceTurn>>(emptyList())
    val micConversation = _micConversation.asStateFlow()

    private var listeningJob: Job? = null
    private var speakingJob: Job? = null
    private var isVoiceScreenActive = false

    private var lastSpokenMessageId: String? = null

    init {
        viewModelScope.launch {
            ttsManager.initializeCurrentProvider()
        }

        // Sync with NodeRuntime chat messages for history if in node chat mode
        viewModelScope.launch {
            nodeRuntime.chatMessages.collect { messages ->
                val voiceTurns = messages.map { msg ->
                    VoiceTurn(
                        id = msg.id,
                        text = msg.content.joinToString("\n") { it.text ?: "" }.trim(),
                        isUser = msg.role.equals("user", ignoreCase = true),
                        timestamp = msg.timestampMs ?: System.currentTimeMillis()
                    )
                }
                _micConversation.value = voiceTurns

                // Auto-speak new assistant messages
                val last = messages.lastOrNull()
                if (last != null && !last.role.equals("user", ignoreCase = true)) {
                    if (last.id != lastSpokenMessageId) {
                        val text = last.content.joinToString("\n") { it.text ?: "" }.trim()
                        if (text.isNotEmpty() && isVoiceScreenActive) {
                            lastSpokenMessageId = last.id
                            speak(text)
                        }
                    }
                } else if (last != null && last.role.equals("user", ignoreCase = true)) {
                    // If user speaks, stop current TTS
                    stopSpeaking()
                }
            }
        }

        viewModelScope.launch {
            nodeRuntime.pendingRunCount.collect { count ->
                _micIsSending.value = count > 0
                if (count > 0) {
                    _micStatusText.value = "Thinking..."
                } else if (!_micEnabled.value) {
                    _micStatusText.value = ""
                }
            }
        }

        viewModelScope.launch {
            nodeRuntime.chatStreamingAssistantText.collect { text ->
                if (!text.isNullOrBlank()) {
                    _micStatusText.value = "Speaking..."
                    // Update latest turn if it's an assistant streaming turn
                    val current = _micConversation.value.toMutableList()
                    val last = current.lastOrNull()
                    if (last != null && !last.isUser && last.isStreaming) {
                        current[current.size - 1] = last.copy(text = text)
                    } else {
                        current.add(VoiceTurn(text = text, isUser = false, isStreaming = true))
                    }
                    _micConversation.value = current
                }
            }
        }

        viewModelScope.launch {
            nodeRuntime.chatError.collect { error ->
                if (!error.isNullOrBlank()) {
                    _micStatusText.value = "Error: $error"
                    _micIsSending.value = false
                }
            }
        }
    }

    fun setVoiceScreenActive(active: Boolean) {
        isVoiceScreenActive = active
        if (!active) {
            stopListening()
            stopSpeaking()
        }
    }

    fun setSpeakerEnabled(enabled: Boolean) {
        _speakerEnabled.value = enabled
        settings.ttsEnabled = enabled
        if (!enabled) {
            stopSpeaking()
        }
    }

    fun toggleMic() {
        if (_micCooldown.value) return

        if (_micEnabled.value) {
            stopListening()
        } else {
            startListening()
        }

        viewModelScope.launch {
            _micCooldown.value = true
            delay(500)
            _micCooldown.value = false
        }
    }

    fun startListening() {
        if (_micEnabled.value) return
        stopSpeaking()

        listeningJob?.cancel()
        listeningJob = viewModelScope.launch {
            _micEnabled.value = true
            _micStatusText.value = "Listening..."
            _micLiveTranscript.value = ""

            try {
                speechManager.startListening(settings.speechLanguage.ifEmpty { null }).collect { result ->
                    when (result) {
                        is SpeechResult.RmsChanged -> {
                            _micInputLevel.value = result.rmsdB
                        }
                        is SpeechResult.PartialResult -> {
                            _micLiveTranscript.value = result.text
                        }
                        is SpeechResult.Result -> {
                            _micLiveTranscript.value = null
                            _micEnabled.value = false
                            _micStatusText.value = "Processing..."
                            sendToAssistant(result.text)
                        }
                        is SpeechResult.Error -> {
                            _micStatusText.value = result.message
                            _micEnabled.value = false
                            _micLiveTranscript.value = null
                            _micInputLevel.value = 0f
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Speech recognition error", e)
                _micEnabled.value = false
                _micStatusText.value = "Error"
            }
        }
    }

    fun stopListening() {
        listeningJob?.cancel()
        _micEnabled.value = false
        _micStatusText.value = ""
        _micLiveTranscript.value = null
        _micInputLevel.value = 0f
    }

    private fun sendToAssistant(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                nodeRuntime.sendChat(text, "low", emptyList())
            } catch (e: Exception) {
                _micStatusText.value = "Send failed"
            }
        }
    }

    private fun stopSpeaking() {
        speakingJob?.cancel()
        ttsManager.stop()
    }

    fun speak(text: String) {
        if (!_speakerEnabled.value || !isVoiceScreenActive) return

        speakingJob?.cancel()
        speakingJob = viewModelScope.launch {
            val cleanText = TTSUtils.stripMarkdownForSpeech(text)
            ttsManager.speakWithProgress(cleanText).collect { state ->
                when (state) {
                    is TTSState.Speaking -> {
                        _micStatusText.value = "Speaking..."
                    }
                    is TTSState.Done -> {
                        _micStatusText.value = ""
                    }
                    is TTSState.Error -> {
                        _micStatusText.value = "TTS Error"
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        stopSpeaking()
    }
}
