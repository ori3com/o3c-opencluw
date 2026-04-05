package com.openclaw.assistant.ui.cron

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.assistant.OpenClawApplication
import com.openclaw.assistant.node.NodeRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CronUiState {
    object Disconnected : CronUiState()
    object Loading : CronUiState()
    data class Success(val jobs: List<NodeRuntime.CronJob>) : CronUiState()
    data class Error(val message: String) : CronUiState()
}

class CronViewModel(app: Application) : AndroidViewModel(app) {

    private val runtime: NodeRuntime = (app as OpenClawApplication).nodeRuntime

    private val _uiState = MutableStateFlow<CronUiState>(CronUiState.Disconnected)
    val uiState: StateFlow<CronUiState> = _uiState.asStateFlow()

    val isConnected = runtime.isConnected

    fun refresh() {
        if (!runtime.isConnected.value) {
            _uiState.value = CronUiState.Disconnected
            return
        }
        viewModelScope.launch {
            _uiState.value = CronUiState.Loading
            try {
                val jobs = runtime.listCronJobs()
                _uiState.value = CronUiState.Success(jobs)
            } catch (e: Throwable) {
                _uiState.value = CronUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun create(name: String, schedule: String, command: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                runtime.createCronJob(name, schedule, command, enabled)
                refresh()
            } catch (e: Throwable) {
                _uiState.value = CronUiState.Error(e.message ?: "Failed to create")
            }
        }
    }

    fun update(id: String, name: String, schedule: String, command: String, enabled: Boolean) {
        viewModelScope.launch {
            runtime.updateCronJob(id, name, schedule, command, enabled)
            refresh()
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runtime.deleteCronJob(id)
            refresh()
        }
    }
}
