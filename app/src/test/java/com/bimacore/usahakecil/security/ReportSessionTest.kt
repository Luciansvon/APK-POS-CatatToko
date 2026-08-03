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
}
