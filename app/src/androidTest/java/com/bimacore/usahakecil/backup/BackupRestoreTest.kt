package com.bimacore.usahakecil.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.data.BusinessProfileEntity
import com.bimacore.usahakecil.data.CategoryEntity
import com.bimacore.usahakecil.data.MIGRATION_1_2
import com.bimacore.usahakecil.data.MIGRATION_2_3
import com.bimacore.usahakecil.data.MIGRATION_3_4
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.data.ProductEntity
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {
    private lateinit var context: Context
    private lateinit var database: PosDatabase
    private val databaseName = "backup-restore-test.db"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun backup_then_restore_returns_original_data() = runBlocking {
        database.profileDao().saveProfile(
            BusinessProfileEntity(
                businessUid = "test-business",
                businessName = "Usaha Awal",
                businessType = "RETAIL",
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        database.catalogDao().insertCategory(
            CategoryEntity(1, "Barang", "inventory", 1),
        )
        database.catalogDao().insertProduct(
            ProductEntity(
                id = 1,
                categoryId = 1,
                name = "Produk Awal",
                basePrice = 10_000,
                stock = 5,
                stockTrackingEnabled = true,
                hasVariants = false,
                lowStockThreshold = 1,
                imageUri = null,
                sortOrder = 1,
            ),
        )
        val manager = BackupManager(
            context = context,
            currentDatabase = { database },
            closeDatabase = { database.close() },
            reopenDatabase = { openDatabase().also { database = it } },
            clock = { 100L },
            databaseName = databaseName,
        )
        val backupUri = manager.createBackup()
        val preview = manager.preview(backupUri)
        database.catalogDao().updateProduct(
            requireNotNull(database.catalogDao().getProduct(1)).copy(
                name = "Produk Berubah",
                stock = 99,
            ),
        )

        manager.restore(preview)

        val restored = requireNotNull(database.catalogDao().getProduct(1))
        assertEquals("Produk Awal", restored.name)
        assertEquals(5, restored.stock)
        assertEquals("Usaha Awal", database.profileDao().getProfile()?.businessName)
    }

    @Test
    fun corrupted_backup_is_rejected_before_restore() = runBlocking {
        database.profileDao().saveProfile(
            BusinessProfileEntity(
                businessUid = "test-business",
                businessName = "Usaha Awal",
                businessType = "RETAIL",
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        val manager = BackupManager(
            context = context,
            currentDatabase = { database },
            closeDatabase = { database.close() },
            reopenDatabase = { openDatabase().also { database = it } },
            clock = { 200L },
            databaseName = databaseName,
        )
        val backupUri = manager.createBackup()
        context.contentResolver.openFileDescriptor(backupUri, "rw")!!.use { descriptor ->
            FileOutputStream(descriptor.fileDescriptor).use { output ->
                output.write("rusak".toByteArray())
                output.fd.sync()
            }
        }

        assertTrue(runCatching { manager.preview(backupUri) }.isFailure)
        assertEquals("Usaha Awal", database.profileDao().getProfile()?.businessName)
    }

    @Test
    fun restore_preserves_current_owner_PIN() = runBlocking {
        database.profileDao().saveProfile(
            BusinessProfileEntity(
                businessUid = "test-business",
                businessName = "Usaha Awal",
                businessType = "RETAIL",
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        database.securityDao().saveReportSecurity(
            com.bimacore.usahakecil.data.ReportSecurityEntity(
                pinHash = "hash-A",
                salt = "salt-A",
                updatedAt = 1,
            )
        )
        val manager = BackupManager(
            context = context,
            currentDatabase = { database },
            closeDatabase = { database.close() },
            reopenDatabase = { openDatabase().also { database = it } },
            clock = { 100L },
            databaseName = databaseName,
        )
        val backupUri = manager.createBackup()
        val preview = manager.preview(backupUri)

        database.securityDao().saveReportSecurity(
            com.bimacore.usahakecil.data.ReportSecurityEntity(
                pinHash = "hash-B",
                salt = "salt-B",
                updatedAt = 2,
            )
        )

        manager.restore(preview)

        val security = database.securityDao().getReportSecurity()
        assertEquals("hash-B", security?.pinHash)
        assertEquals("salt-B", security?.salt)
    }

    @Test
    fun restore_validates_manifest_identity_matches_database() = runBlocking {
        database.profileDao().saveProfile(
            BusinessProfileEntity(
                businessUid = "business-B",
                businessName = "Usaha B",
                businessType = "RETAIL",
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        val manager = BackupManager(
            context = context,
            currentDatabase = { database },
            closeDatabase = { database.close() },
            reopenDatabase = { openDatabase().also { database = it } },
            clock = { 100L },
            databaseName = databaseName,
        )
        manager.createBackup()

        val source = context.getDatabasePath(databaseName)
        val bytes = source.readBytes()
        val tamperedManifest = BackupManifest.create(
            schemaVersion = 4,
            businessUid = "business-A",
            businessName = "Usaha A",
            businessType = "RETAIL",
            createdAt = 100L,
            databaseBytes = bytes,
        )

        val directory = java.io.File(context.cacheDir, "backups").apply { mkdirs() }
        val output = java.io.File(directory, "tampered.ukbackup")
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(output)).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("manifest.txt"))
            zip.write(tamperedManifest.serialize().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(java.util.zip.ZipEntry("database.db"))
            zip.write(bytes)
            zip.closeEntry()
        }

        val tamperedUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output,
        )
        val preview = manager.preview(tamperedUri)

        val result = runCatching { manager.restore(preview) }
        assertTrue(result.isFailure)
        assertEquals("Identitas usaha pada salinan tidak sesuai dengan keterangan", result.exceptionOrNull()?.message)
    }

    private fun openDatabase(): PosDatabase =
        Room.databaseBuilder(context, PosDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
}
