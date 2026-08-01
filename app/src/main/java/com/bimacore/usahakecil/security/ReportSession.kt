package com.bimacore.usahakecil.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportSession {
    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()

    val isUnlocked: Boolean
        get() = _unlocked.value

    @Synchronized
    fun unlock() {
        _unlocked.value = true
    }

    @Synchronized
    fun lock() {
        _unlocked.value = false
    }

    @Synchronized
    fun beginExternalOwnerFlow() {
        requireOwner()
    }

    @Synchronized
    fun endExternalOwnerFlow() {
    }

    fun requireOwner() {
        check(_unlocked.value) { "Akses ditolak: Sesi Owner belum terverifikasi" }
    }
}
