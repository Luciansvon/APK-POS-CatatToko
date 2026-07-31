package com.bimacore.usahakecil.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportSession {
    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()

    val isUnlocked: Boolean
        get() = _unlocked.value

    fun unlock() {
        _unlocked.value = true
    }

    fun lock() {
        _unlocked.value = false
    }

    fun requireOwner() {
        check(_unlocked.value) { "Akses ditolak: Sesi Owner belum terverifikasi" }
    }
}

