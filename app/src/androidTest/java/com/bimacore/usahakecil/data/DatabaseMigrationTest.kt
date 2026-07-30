package com.bimacore.usahakecil.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PosDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migration_1_2_preserves_existing_catalog_and_sales() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO categories (id, name, iconKey, sortOrder)
                VALUES (1, 'Sembako', 'store', 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO products (
                    id, categoryId, name, basePrice, stock, stockTrackingEnabled,
                    hasVariants, lowStockThreshold, imageUri, sortOrder
                ) VALUES (1, 1, 'Beras Lama', 25000, 3, 1, 0, 1, NULL, 1)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO sales (
                    id, receiptNumber, businessName, createdAt, paymentMethod,
                    total, amountReceived, changeAmount
                ) VALUES (1, 'INV-LAMA', 'Warung Lama', 1000, 'CASH', 25000, 30000, 5000)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            MIGRATION_1_2,
        )

        migrated.query("SELECT name, stock FROM products WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("Beras Lama", it.getString(0))
            assertEquals(3, it.getInt(1))
        }
        migrated.query("SELECT receiptNumber, total FROM sales WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("INV-LAMA", it.getString(0))
            assertEquals(25_000L, it.getLong(1))
        }
        migrated.close()
    }

    companion object {
        private const val TEST_DATABASE = "migration-1-2-test"
    }
}
