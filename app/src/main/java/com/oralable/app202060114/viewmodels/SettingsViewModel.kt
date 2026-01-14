package com.oralable.app202060114.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val showDeveloperMessage: Boolean = false,
    val developerTapCount: Int = 0
)

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun handleDeveloperTap() {
        val newCount = _uiState.value.developerTapCount + 1
        if (newCount >= 7) {
            _uiState.value = _uiState.value.copy(showDeveloperMessage = true, developerTapCount = 0)
        } else {
            _uiState.value = _uiState.value.copy(developerTapCount = newCount)
        }
    }
}
