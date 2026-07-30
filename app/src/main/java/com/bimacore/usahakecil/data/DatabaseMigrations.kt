package com.bimacore.usahakecil.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE categories ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        database.execSQL("ALTER TABLE products ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE products ADD COLUMN unitLabel TEXT NOT NULL DEFAULT 'pcs'")
        database.execSQL("ALTER TABLE products ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        database.execSQL("ALTER TABLE product_variants ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
        database.execSQL("ALTER TABLE product_variants ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        database.execSQL("ALTER TABLE sales ADD COLUMN customerId INTEGER")
        database.execSQL("ALTER TABLE sales ADD COLUMN settlementStatus TEXT NOT NULL DEFAULT 'PAID'")
        database.execSQL("ALTER TABLE sales ADD COLUMN orderStatus TEXT NOT NULL DEFAULT 'COMPLETED'")
        database.execSQL("ALTER TABLE sales ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE sales ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

        database.execSQL("ALTER TABLE sale_items ADD COLUMN unitLabel TEXT NOT NULL DEFAULT 'pcs'")
        database.execSQL("ALTER TABLE sale_items ADD COLUMN note TEXT NOT NULL DEFAULT ''")

        database.execSQL("ALTER TABLE stock_movements ADD COLUMN referenceType TEXT NOT NULL DEFAULT 'SALE'")
        database.execSQL("ALTER TABLE stock_movements ADD COLUMN referenceId INTEGER")
        database.execSQL("ALTER TABLE stock_movements ADD COLUMN unitLabel TEXT NOT NULL DEFAULT 'pcs'")
        database.execSQL("ALTER TABLE stock_movements ADD COLUMN baseQuantityDelta INTEGER NOT NULL DEFAULT 0")
        database.execSQL("UPDATE stock_movements SET baseQuantityDelta = quantityDelta")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS business_profile (
                id INTEGER NOT NULL,
                businessUid TEXT NOT NULL,
                businessName TEXT NOT NULL,
                businessType TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS unit_conversions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                label TEXT NOT NULL,
                factorToBase INTEGER NOT NULL,
                salePrice INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_unit_conversions_productId ON unit_conversions(productId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS price_tiers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                minimumBaseQuantity INTEGER NOT NULL,
                unitPrice INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_price_tiers_productId ON price_tiers(productId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS parties (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                kind TEXT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_parties_kind ON parties(kind)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_parties_name ON parties(name)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS purchases (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                supplierId INTEGER NOT NULL,
                supplierName TEXT NOT NULL,
                invoiceNumber TEXT NOT NULL,
                total INTEGER NOT NULL,
                amountPaid INTEGER NOT NULL,
                settlementStatus TEXT NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_purchases_supplierId ON purchases(supplierId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_purchases_invoiceNumber ON purchases(invoiceNumber)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS purchase_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                purchaseId INTEGER NOT NULL,
                productId INTEGER NOT NULL,
                variantId INTEGER,
                productName TEXT NOT NULL,
                variantName TEXT,
                unitLabel TEXT NOT NULL,
                factorToBase INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                baseQuantity INTEGER NOT NULL,
                unitCost INTEGER NOT NULL,
                subtotal INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_purchaseId ON purchase_items(purchaseId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_productId ON purchase_items(productId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_purchase_items_variantId ON purchase_items(variantId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cash_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                amount INTEGER NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL,
                paymentMethod TEXT NOT NULL,
                referenceType TEXT,
                referenceId INTEGER,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_cash_entries_type ON cash_entries(type)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_cash_entries_referenceType ON cash_entries(referenceType)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_cash_entries_referenceId ON cash_entries(referenceId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                kind TEXT NOT NULL,
                partyId INTEGER NOT NULL,
                partyName TEXT NOT NULL,
                sourceType TEXT NOT NULL,
                sourceId INTEGER NOT NULL,
                originalAmount INTEGER NOT NULL,
                paidAmount INTEGER NOT NULL,
                settlementStatus TEXT NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_kind ON debts(kind)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_partyId ON debts(partyId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_sourceType ON debts(sourceType)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_debts_sourceId ON debts(sourceId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS debt_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                debtId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                paymentMethod TEXT NOT NULL,
                note TEXT NOT NULL,
                paidAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON debt_payments(debtId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS employees (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                scheme TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_scheme ON employees(scheme)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_employees_name ON employees(name)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS wage_rates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                effectiveAt INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_wage_rates_employeeId ON wage_rates(employeeId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_wage_rates_effectiveAt ON wage_rates(effectiveAt)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attendance_records (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId INTEGER NOT NULL,
                workDate INTEGER NOT NULL,
                status TEXT NOT NULL,
                rateSnapshot INTEGER NOT NULL,
                overtime INTEGER NOT NULL,
                bonus INTEGER NOT NULL,
                deduction INTEGER NOT NULL,
                advance INTEGER NOT NULL,
                netPay INTEGER NOT NULL,
                note TEXT NOT NULL,
                isPaid INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_records_employeeId ON attendance_records(employeeId)")
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_records_employeeId_workDate ON attendance_records(employeeId, workDate)",
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS freelance_jobs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId INTEGER NOT NULL,
                title TEXT NOT NULL,
                agreedAmount INTEGER NOT NULL,
                paidAmount INTEGER NOT NULL,
                status TEXT NOT NULL,
                workDate INTEGER NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_freelance_jobs_employeeId ON freelance_jobs(employeeId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_freelance_jobs_status ON freelance_jobs(status)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS worker_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                employeeId INTEGER NOT NULL,
                referenceType TEXT NOT NULL,
                referenceId INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                note TEXT NOT NULL,
                paidAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_worker_payments_employeeId ON worker_payments(employeeId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_worker_payments_referenceType ON worker_payments(referenceType)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_worker_payments_referenceId ON worker_payments(referenceId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS toppings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                productId INTEGER NOT NULL,
                label TEXT NOT NULL,
                price INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_toppings_productId ON toppings(productId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recipe_ingredients (
                menuProductId INTEGER NOT NULL,
                ingredientProductId INTEGER NOT NULL,
                quantityPerMenu INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(menuProductId, ingredientProductId)
            )
            """.trimIndent(),
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_recipe_ingredients_ingredientProductId ON recipe_ingredients(ingredientProductId)",
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cart_line_notes (
                lineId TEXT NOT NULL,
                note TEXT NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(lineId)
            )
            """.trimIndent(),
        )
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cart_line_toppings (
                lineId TEXT NOT NULL,
                toppingId INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(lineId, toppingId)
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_cart_line_toppings_toppingId ON cart_line_toppings(toppingId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sale_item_toppings (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                saleItemId INTEGER NOT NULL,
                toppingId INTEGER NOT NULL,
                toppingName TEXT NOT NULL,
                unitPrice INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                subtotal INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sale_item_toppings_saleItemId ON sale_item_toppings(saleItemId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_sale_item_toppings_toppingId ON sale_item_toppings(toppingId)")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS report_security (
                id INTEGER NOT NULL,
                saltBase64 TEXT NOT NULL,
                hashBase64 TEXT NOT NULL,
                iterations INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent(),
        )
    }
}
