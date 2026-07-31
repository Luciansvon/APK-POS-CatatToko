package com.bimacore.usahakecil.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSessionTest {
    @Test
    fun external_owner_flow_prevents_automatic_lock_until_it_finishes() {
        val session = ReportSession()
        session.unlock()

        session.beginExternalOwnerFlow()
        session.lockUnlessExternalOwnerFlow()

        assertTrue(session.isUnlocked)

        session.endExternalOwnerFlow()
        session.lockUnlessExternalOwnerFlow()

        assertFalse(session.isUnlocked)
    }
}
