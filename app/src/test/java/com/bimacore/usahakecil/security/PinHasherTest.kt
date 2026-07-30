package com.bimacore.usahakecil.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun `pin is salted and never stored as plaintext`() {
        val first = PinHasher.create("123456")
        val second = PinHasher.create("123456")

        assertNotEquals("123456", first.hashBase64)
        assertNotEquals(first.saltBase64, second.saltBase64)
        assertNotEquals(first.hashBase64, second.hashBase64)
    }

    @Test
    fun `pin verification accepts correct value and rejects wrong value`() {
        val record = PinHasher.create("246810")

        assertTrue(PinHasher.verify("246810", record))
        assertFalse(PinHasher.verify("246811", record))
    }

    @Test
    fun `report session starts locked and can be locked again`() {
        val session = ReportSession()

        assertFalse(session.isUnlocked)
        session.unlock()
        assertTrue(session.isUnlocked)
        session.lock()
        assertFalse(session.isUnlocked)
    }

    @Test
    fun `owner access state follows the report session`() {
        val session = ReportSession()

        assertFalse(session.unlocked.value)
        session.unlock()
        assertTrue(session.unlocked.value)
        session.lock()
        assertFalse(session.unlocked.value)
    }
}
