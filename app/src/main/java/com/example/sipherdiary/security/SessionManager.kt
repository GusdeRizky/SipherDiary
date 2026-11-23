package com.example.sipherdiary.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SessionState {
    Locked,
    Unlocked,
    DecoyUnlocked
}

object SessionManager {
    private val _state = MutableStateFlow(SessionState.Locked)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    fun unlock() {
        _state.value = SessionState.Unlocked
    }

    fun unlockDecoy() {
        _state.value = SessionState.DecoyUnlocked
    }

    fun lock() {
        _state.value = SessionState.Locked
    }
}