package com.openclaw.assistant.ui

import android.graphics.Color as AndroidColor
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.openclaw.assistant.R
import com.openclaw.assistant.node.CanvasController
import com.openclaw.assistant.node.NodeRuntime
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun CanvasScreen(
    canvasController: CanvasController,
    nodeRuntime: NodeRuntime,
    modifier: Modifier = Modifier
) {
    val nodeRuntimeRef = remember { mutableStateOf(nodeRuntime) }
    SideEffect { nodeRuntimeRef.value = nodeRuntime }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).also { frame ->
                // 1. WebView — first child, rendered below
                val webView = WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }
                    webViewClient = CanvasWebViewClient(canvasController)
                }
                canvasController.attach(webView)
                frame.addView(webView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))

                // 2. ComposeView overlay — second child, always on top of WebView.
                //    Explicit transparent background so WebView shows through when Canvas is active.
                val overlayView = ComposeView(context).apply {
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    setViewCompositionStrategy(
                        ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool
                    )
                    setContent {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Spacer(modifier = Modifier.weight(1f))
                            CanvasChatBar(
                                nodeRuntimeRef = nodeRuntimeRef,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                frame.addView(overlayView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }
        },
        onRelease = { frame ->
            val webView = (0 until frame.childCount)
                .map { frame.getChildAt(it) }
                .filterIsInstance<WebView>()
                .firstOrNull()
            webView?.stopLoading()
            canvasController.detach()
            webView?.destroy()
        }
    )
}


@Composable
private fun CanvasChatBar(
    nodeRuntimeRef: androidx.compose.runtime.State<NodeRuntime>,
    modifier: Modifier = Modifier
) {
    val nodeRuntime = nodeRuntimeRef.value
    val healthOk by nodeRuntime.chatHealthOk.collectAsState()
    val streamingText by nodeRuntime.chatStreamingAssistantText.collectAsState()
    val pendingTools by nodeRuntime.chatPendingToolCalls.collectAsState()
    val messages by nodeRuntime.chatMessages.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var lastAiText by remember { mutableStateOf<String?>(null) }

    val isAiBusy = isSending || streamingText != null || pendingTools.isNotEmpty()

    // Extract text from the last assistant message to show as response
    LaunchedEffect(messages) {
        val lastAssistant = messages.lastOrNull { it.role == "assistant" }
        val text = lastAssistant?.content?.firstOrNull()?.let { c ->
            (c as? JsonObject)?.get("text")?.let { (it as? JsonPrimitive)?.content }
        }
        if (!text.isNullOrBlank()) lastAiText = text
    }

    // Once AI starts responding, clear isSending
    LaunchedEffect(streamingText, pendingTools.size) {
        if (streamingText != null || pendingTools.isNotEmpty()) isSending = false
    }
    // Safety timeout in case gateway never responds
    LaunchedEffect(isSending) {
        if (isSending) {
            delay(15_000L)
            isSending = false
        }
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isBlank() || !healthOk || isAiBusy) return
        inputText = ""
        lastAiText = null
        isSending = true
        // Append a Canvas-use hint so the AI responds visually on Canvas when possible.
        val canvasHint = "\n\n(You are responding from the Canvas tab. " +
            "Please use Canvas tools to display your response visually whenever possible.)"
        nodeRuntimeRef.value.sendChat(text + canvasHint, "low", emptyList())
    }

    // Determine status label and whether to show spinner
    val statusLabel: String? = when {
        isSending                   -> stringResource(R.string.canvas_status_sending)
        pendingTools.isNotEmpty()   -> stringResource(R.string.canvas_status_tool)
        streamingText != null       -> stringResource(R.string.canvas_status_thinking)
        !healthOk                   -> stringResource(R.string.canvas_chat_not_connected)
        else                        -> null
    }
    val showSpinner = isAiBusy

    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // ── Status row ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = statusLabel != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (statusLabel != null) {
                    val (bg, fg) = if (!healthOk && !isAiBusy)
                        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showSpinner) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = fg
                            )
                        }
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = fg
                        )
                    }
                }
            }

            // ── AI response text (streaming or last message) ─────────────
            val displayText = streamingText ?: if (!isAiBusy) lastAiText else null
            AnimatedVisibility(
                visible = displayText != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (displayText != null) {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── Input row ────────────────────────────────────────────────
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(text = stringResource(R.string.canvas_chat_hint))
                    },
                    enabled = healthOk && !isAiBusy,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { sendMessage() },
                    enabled = healthOk && !isAiBusy && inputText.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.canvas_chat_send)
                    )
                }
            }
        }
    }
}

private class CanvasWebViewClient(
    private val canvasController: CanvasController
) : WebViewClient() {

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        canvasController.onPageFinished()
    }

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        val token = canvasController.gatewayToken?.takeIf { it.isNotBlank() } ?: return null
        val origin = canvasController.gatewayOrigin ?: return null
        if (!request.url.toString().startsWith(origin)) return null

        return try {
            val conn = URL(request.url.toString()).openConnection() as HttpURLConnection
            request.requestHeaders.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.connect()
            WebResourceResponse(
                conn.contentType?.substringBefore(";") ?: "text/html",
                conn.contentEncoding ?: "utf-8",
                conn.inputStream
            )
        } catch (_: Exception) {
            null
        }
    }
}
