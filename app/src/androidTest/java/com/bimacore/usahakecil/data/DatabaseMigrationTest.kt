package com.bimacore.usahakecil.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun migration_1_6_preserves_existing_data_and_creates_barcode_table() {
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
            execSQL(
                """
                INSERT INTO sale_items (
                    id, saleId, productId, variantId, productName, variantName,
                    categoryName, unitPrice, quantity, subtotal
                ) VALUES (1, 1, 1, NULL, 'Beras Lama', NULL, 'Sembako', 25000, 3, 75000)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            6,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
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
        migrated.query("SELECT shiftId FROM sales WHERE id = 1").use {
            it.moveToFirst()
            assertTrue(it.isNull(0))
        }
        migrated.query("SELECT quantity, baseQuantity FROM sale_items WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(3, it.getInt(0))
            assertEquals(3, it.getInt(1))
        }
        migrated.query("SELECT COUNT(*) FROM shifts").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM history_import_batches").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.query("SELECT COUNT(*) FROM product_barcodes").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun migration_2_6_validates_schema() {
        helper.createDatabase("migration-2-6-test", 2).close()
        val migrated = helper.runMigrationsAndValidate(
            "migration-2-6-test",
            6,
            true,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )
        migrated.close()
    }

    @Test
    fun migration_3_6_validates_schema() {
        helper.createDatabase("migration-3-6-test", 3).close()
        val migrated = helper.runMigrationsAndValidate(
            "migration-3-6-test",
            6,
            true,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )
        migrated.close()
    }

    @Test
    fun migration_4_6_validates_schema() {
        helper.createDatabase("migration-4-6-test", 4).close()
        val migrated = helper.runMigrationsAndValidate(
            "migration-4-6-test",
            6,
            true,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )
        migrated.close()
    }

    @Test
    fun migration_5_6_preserves_products_and_enforces_unique_barcode() {
        helper.createDatabase("migration-5-6-test", 5).apply {
            execSQL("INSERT INTO categories (id, name, iconKey, sortOrder, isActive, updatedAt) VALUES (1, 'Barang', 'store', 1, 1, 0)")
            execSQL(
                "INSERT INTO products (id, categoryId, name, basePrice, stock, stockTrackingEnabled, hasVariants, lowStockThreshold, imageUri, sortOrder, isActive, unitLabel, updatedAt) VALUES (1, 1, 'Produk Lama', 1000, 2, 1, 0, 1, NULL, 1, 1, 'pcs', 0)",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(
            "migration-5-6-test",
            6,
            true,
            MIGRATION_5_6,
        )
        migrated.execSQL(
            "INSERT INTO product_barcodes (barcode, productId, variantId, unitId, isActive, createdAt, updatedAt) VALUES ('00123', 1, NULL, NULL, 1, 1, 1)",
        )
        assertTrue(
            runCatching {
                migrated.execSQL(
                    "INSERT INTO product_barcodes (barcode, productId, variantId, unitId, isActive, createdAt, updatedAt) VALUES ('00123', 1, NULL, NULL, 1, 2, 2)",
                )
            }.isFailure,
        )
        migrated.query("SELECT name FROM products WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("Produk Lama", it.getString(0))
        }
        migrated.close()
    }

    companion object {
        private const val TEST_DATABASE = "migration-1-6-test"
    }
}
