package com.oralable.app202060114.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShareUiState(
    val shareMessage: String = ""
)

class ShareViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    fun shareCsv() {
        _uiState.value = _uiState.value.copy(shareMessage = "Sharing CSV data...")
    }
}
