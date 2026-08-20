package com.bimacore.usahakecil.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSessionTest {
    @Test
    fun owner_session_starts_locked_and_external_flow_locks_on_return() {
        val session = ReportSession()

        assertFalse(session.isUnlocked)
        session.unlock()

        session.beginExternalOwnerFlow()
        session.endExternalOwnerFlow()

        assertFalse(session.isUnlocked)
    }

    @Test
    fun a_new_session_never_inherits_previous_unlock_state() {
        val session = ReportSession()
        session.unlock()
        val reopened = ReportSession()

        assertTrue(session.isUnlocked)
        assertFalse(reopened.isUnlocked)
    }

    @Test
    fun owner_session_locks_after_five_failed_unlock_attempts() {
        val session = ReportSession()

        repeat(4) { index ->
            assertTrue(session.recordFailedUnlock(index.toLong()) == 0L)
        }
        assertTrue(session.recordFailedUnlock(4L) > 0L)
        assertTrue(session.unlockAttemptRemainingMillis(4L) > 0L)
        session.resetUnlockAttempts()
        assertTrue(session.unlockAttemptRemainingMillis(4L) == 0L)
    }
}
