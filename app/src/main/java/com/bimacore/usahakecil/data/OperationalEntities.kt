package com.bimacore.usahakecil.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: Int = 1,
    val businessUid: String,
    val businessName: String,
    val businessType: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "unit_conversions",
    indices = [Index("productId")],
)
data class UnitConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val label: String,
    val factorToBase: Int,
    val salePrice: Long,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "price_tiers",
    indices = [Index("productId")],
)
data class PriceTierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val minimumBaseQuantity: Int,
    val unitPrice: Long,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "parties",
    indices = [Index("kind"), Index("name")],
)
data class PartyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val name: String,
    val phone: String,
    val address: String,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "purchases",
    indices = [Index("supplierId"), Index(value = ["invoiceNumber"], unique = true)],
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val invoiceNumber: String,
    val total: Long,
    val amountPaid: Long,
    val settlementStatus: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "purchase_items",
    indices = [Index("purchaseId"), Index("productId"), Index("variantId")],
)
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val variantId: Long?,
    val productName: String,
    val variantName: String?,
    val unitLabel: String,
    val factorToBase: Int,
    val quantity: Int,
    val baseQuantity: Int,
    val unitCost: Long,
    val subtotal: Long,
)

@Entity(
    tableName = "cash_entries",
    indices = [Index("type"), Index("referenceType"), Index("referenceId"), Index("shiftId")],
)
data class CashEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amount: Long,
    val category: String,
    val note: String,
    val paymentMethod: String,
    val referenceType: String?,
    val referenceId: Long?,
    val createdAt: Long,
    val shiftId: Long? = null,
)

@Entity(
    tableName = "shifts",
    indices = [
        Index("status"),
        Index("openedAt"),
        Index(value = ["openSlot"], unique = true),
    ],
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cashierName: String,
    val openedAt: Long,
    val openingCash: Long,
    @ColumnInfo(defaultValue = "''") val openingNote: String = "",
    @ColumnInfo(defaultValue = "'OPEN'") val status: String = "OPEN",
    val closedAt: Long? = null,
    val closingCash: Long? = null,
    @ColumnInfo(defaultValue = "''") val closingNote: String = "",
    @ColumnInfo(defaultValue = "0") val totalSales: Long = 0,
    @ColumnInfo(defaultValue = "0") val cashSales: Long = 0,
    @ColumnInfo(defaultValue = "0") val nonCashSales: Long = 0,
    @ColumnInfo(defaultValue = "0") val otherCashIn: Long = 0,
    @ColumnInfo(defaultValue = "0") val cashOut: Long = 0,
    @ColumnInfo(defaultValue = "0") val refundAmount: Long = 0,
    val expectedCash: Long? = null,
    val cashDifference: Long? = null,
    val openSlot: Int? = null,
)

@Entity(
    tableName = "debts",
    indices = [Index("kind"), Index("partyId"), Index("sourceType"), Index("sourceId")],
)
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val partyId: Long,
    val partyName: String,
    val sourceType: String,
    val sourceId: Long,
    val originalAmount: Long,
    val paidAmount: Long,
    val settlementStatus: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "debt_payments",
    indices = [Index("debtId")],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val paymentMethod: String,
    val note: String,
    val paidAt: Long,
)

@Entity(
    tableName = "employees",
    indices = [Index("scheme"), Index("name")],
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val scheme: String,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "wage_rates",
    indices = [Index("employeeId"), Index("effectiveAt")],
)
data class WageRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val amount: Long,
    val effectiveAt: Long,
    val createdAt: Long,
)

@Entity(
    tableName = "attendance_records",
    indices = [Index("employeeId"), Index(value = ["employeeId", "workDate"], unique = true)],
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val workDate: Long,
    val status: String,
    val rateSnapshot: Long,
    val overtime: Long,
    val bonus: Long,
    val deduction: Long,
    val advance: Long,
    val netPay: Long,
    val note: String,
    val isPaid: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "freelance_jobs",
    indices = [Index("employeeId"), Index("status")],
)
data class FreelanceJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val title: String,
    val agreedAmount: Long,
    val paidAmount: Long,
    val status: String,
    val workDate: Long,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "worker_payments",
    indices = [Index("employeeId"), Index("referenceType"), Index("referenceId")],
)
data class WorkerPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val employeeId: Long,
    val referenceType: String,
    val referenceId: Long,
    val amount: Long,
    val note: String,
    val paidAt: Long,
)

@Entity(
    tableName = "toppings",
    indices = [Index("productId")],
)
data class ToppingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val label: String,
    val price: Long,
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "recipe_ingredients",
    primaryKeys = ["menuProductId", "ingredientProductId"],
    indices = [Index("ingredientProductId")],
)
data class RecipeIngredientEntity(
    val menuProductId: Long,
    val ingredientProductId: Long,
    val quantityPerMenu: Int,
    val updatedAt: Long,
)

@Entity(tableName = "cart_line_notes")
data class CartLineNoteEntity(
    @PrimaryKey val lineId: String,
    val note: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "cart_line_toppings",
    primaryKeys = ["lineId", "toppingId"],
    indices = [Index("toppingId")],
)
data class CartLineToppingEntity(
    val lineId: String,
    val toppingId: Long,
    val quantity: Int,
    val updatedAt: Long,
)

@Entity(
    tableName = "sale_item_toppings",
    indices = [Index("saleItemId"), Index("toppingId")],
)
data class SaleItemToppingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleItemId: Long,
    val toppingId: Long,
    val toppingName: String,
    val unitPrice: Long,
    val quantity: Int,
    val subtotal: Long,
)

@Entity(tableName = "report_security")
data class ReportSecurityEntity(
    @PrimaryKey val id: Int = 1,
    val saltBase64: String,
    val hashBase64: String,
    val iterations: Int,
    val updatedAt: Long,
)
