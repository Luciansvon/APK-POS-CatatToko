package com.bimacore.usahakecil.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReportSession {
    companion object {
        private const val MAX_FAILED_UNLOCK_ATTEMPTS = 5
        private const val LOCKOUT_NANOS = 30_000_000_000L
    }

    private val _unlocked = MutableStateFlow(false)
    val unlocked = _unlocked.asStateFlow()

    private var failedUnlockAttempts = 0
    private var unlockLockedUntilNanos = 0L

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
    fun unlockAttemptRemainingMillis(nowNanos: Long = System.nanoTime()): Long {
        val remaining = unlockLockedUntilNanos - nowNanos
        if (remaining <= 0L) {
            unlockLockedUntilNanos = 0L
            return 0L
        }
        return (remaining + 999_999L) / 1_000_000L
    }

    @Synchronized
    fun recordFailedUnlock(nowNanos: Long = System.nanoTime()): Long {
        failedUnlockAttempts++
        if (failedUnlockAttempts < MAX_FAILED_UNLOCK_ATTEMPTS) return 0L
        failedUnlockAttempts = 0
        unlockLockedUntilNanos = nowNanos + LOCKOUT_NANOS
        return LOCKOUT_NANOS / 1_000_000L
    }

    @Synchronized
    fun resetUnlockAttempts() {
        failedUnlockAttempts = 0
        unlockLockedUntilNanos = 0L
    }

    @Synchronized
    fun beginExternalOwnerFlow() {
        requireOwner()
    }

    @Synchronized
    fun endExternalOwnerFlow() {
        lock()
    }

    fun requireOwner() {
        check(_unlocked.value) { "Akses ditolak: Sesi Owner belum terverifikasi" }
    }
}
