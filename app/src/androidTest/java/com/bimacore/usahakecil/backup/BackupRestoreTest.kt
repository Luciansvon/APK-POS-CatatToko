package com.bimacore.usahakecil.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.data.BusinessProfileEntity
import com.bimacore.usahakecil.data.CategoryEntity
import com.bimacore.usahakecil.data.MIGRATION_1_2
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

    private fun openDatabase(): PosDatabase =
        Room.databaseBuilder(context, PosDatabase::class.java, databaseName)
            .addMigrations(MIGRATION_1_2)
            .build()
}
