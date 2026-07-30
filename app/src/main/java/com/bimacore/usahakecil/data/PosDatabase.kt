package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        ProductVariantEntity::class,
        DraftCartEntity::class,
        CartLineEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        StockMovementEntity::class,
        BusinessProfileEntity::class,
        UnitConversionEntity::class,
        PriceTierEntity::class,
        PartyEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        CashEntryEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
        EmployeeEntity::class,
        WageRateEntity::class,
        AttendanceEntity::class,
        FreelanceJobEntity::class,
        WorkerPaymentEntity::class,
        ToppingEntity::class,
        RecipeIngredientEntity::class,
        CartLineNoteEntity::class,
        CartLineToppingEntity::class,
        SaleItemToppingEntity::class,
        ReportSecurityEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun cartDao(): CartDao
    abstract fun saleDao(): SaleDao
    abstract fun stockDao(): StockDao
    abstract fun profileDao(): ProfileDao
    abstract fun inventoryAdminDao(): InventoryAdminDao
    abstract fun operationsDao(): OperationsDao
    abstract fun workforceDao(): WorkforceDao
    abstract fun culinaryDao(): CulinaryDao
    abstract fun securityDao(): SecurityDao
    abstract fun reportDao(): ReportDao

    companion object {
        fun create(context: Context): PosDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                PosDatabase::class.java,
                "usaha-kecil-pos.db",
            )
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
