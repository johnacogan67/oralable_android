package com.oralable.app202060114.bluetooth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ConnectionState(
    val oralableConnected: Boolean = false,
    val anrConnected: Boolean = false
)

object ConnectionManager {
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    fun setOralableConnected(isConnected: Boolean) {
        _connectionState.value = _connectionState.value.copy(oralableConnected = isConnected)
    }

    fun setAnrConnected(isConnected: Boolean) {
        _connectionState.value = _connectionState.value.copy(anrConnected = isConnected)
    }
}
