package com.bimacore.usahakecil.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSessionTest {
    @Test
    fun owner_session_stays_open_until_explicit_lock() {
        val session = ReportSession()
        session.unlock()

        session.beginExternalOwnerFlow()
        session.endExternalOwnerFlow()

        assertTrue(session.isUnlocked)

        session.lock()

        assertFalse(session.isUnlocked)
    }

    @Test
    fun owner_session_can_be_restored_until_explicit_lock() {
        var persisted = false
        val session = ReportSession(
            initiallyUnlocked = persisted,
            onStateChanged = { persisted = it },
        )

        session.unlock()
        val reopened = ReportSession(
            initiallyUnlocked = persisted,
            onStateChanged = { persisted = it },
        )

        assertTrue(reopened.isUnlocked)
        reopened.lock()
        assertFalse(persisted)
    }
}
