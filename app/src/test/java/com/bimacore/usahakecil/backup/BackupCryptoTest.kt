package com.bimacore.usahakecil.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun `encrypted payload needs the same pin and manifest binding`() {
        val payload = "backup data".encodeToByteArray()
        val parameters = BackupCrypto.newParameters()
        val manifest = "manifest-v3".encodeToByteArray()
        val encrypted = BackupCrypto.encrypt(payload, "2468", parameters, manifest)

        val decrypted = BackupCrypto.decrypt(
            payload = encrypted,
            pin = "2468",
            saltBase64 = BackupCrypto.encode(parameters.salt),
            nonceBase64 = BackupCrypto.encode(parameters.nonce),
            associatedData = manifest,
        )

        assertArrayEquals(payload, decrypted)
        assertTrue(
            runCatching {
                BackupCrypto.decrypt(
                    encrypted,
                    "0000",
                    BackupCrypto.encode(parameters.salt),
                    BackupCrypto.encode(parameters.nonce),
                    manifest,
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                BackupCrypto.decrypt(
                    encrypted,
                    "2468",
                    BackupCrypto.encode(parameters.salt),
                    BackupCrypto.encode(parameters.nonce),
                    "manifest-tampered".encodeToByteArray(),
                )
            }.isFailure,
        )
    }
}
