package com.bimacore.usahakecil.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManifestTest {
    @Test
    fun `manifest round trip preserves backup identity`() {
        val database = "database-content".encodeToByteArray()
        val manifest = BackupManifest.create(
            schemaVersion = 2,
            businessUid = "usaha-123",
            businessName = "Warung Bima",
            businessType = "RETAIL",
            createdAt = 123_456L,
            databaseBytes = database,
        )

        assertEquals(manifest, BackupManifest.parse(manifest.serialize()))
        assertTrue(manifest.verify(database))
    }

    @Test
    fun `modified database fails integrity validation`() {
        val database = "database-content".encodeToByteArray()
        val manifest = BackupManifest.create(
            schemaVersion = 2,
            businessUid = "usaha-123",
            businessName = "Warung Bima",
            businessType = "RETAIL",
            createdAt = 123_456L,
            databaseBytes = database,
        )

        assertFalse(manifest.verify(database + 1))
    }
}
