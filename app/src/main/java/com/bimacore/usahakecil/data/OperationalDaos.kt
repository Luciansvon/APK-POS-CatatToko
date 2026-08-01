package com.bimacore.usahakecil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1")
    fun observeProfile(): Flow<BusinessProfileEntity?>

    @Query("SELECT * FROM business_profile WHERE id = 1")
    suspend fun getProfile(): BusinessProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: BusinessProfileEntity)
}

@Dao
interface InventoryAdminDao {
    @Query("SELECT * FROM unit_conversions ORDER BY productId, factorToBase, label")
    fun observeAllUnits(): Flow<List<UnitConversionEntity>>

    @Query("SELECT * FROM unit_conversions WHERE productId = :productId AND isActive = 1 ORDER BY factorToBase, label")
    suspend fun getActiveUnits(productId: Long): List<UnitConversionEntity>

    @Query("SELECT * FROM unit_conversions WHERE productId = :productId ORDER BY factorToBase, label")
    fun observeUnits(productId: Long): Flow<List<UnitConversionEntity>>

    @Query("SELECT * FROM unit_conversions WHERE id = :id")
    suspend fun getUnit(id: Long): UnitConversionEntity?

    @Insert
    suspend fun insertUnit(unit: UnitConversionEntity): Long

    @Update
    suspend fun updateUnit(unit: UnitConversionEntity)

    @Query("SELECT * FROM price_tiers WHERE productId = :productId ORDER BY minimumBaseQuantity")
    fun observePriceTiers(productId: Long): Flow<List<PriceTierEntity>>

    @Query("SELECT * FROM price_tiers WHERE productId = :productId ORDER BY minimumBaseQuantity")
    suspend fun getPriceTiers(productId: Long): List<PriceTierEntity>

    @Query("SELECT * FROM price_tiers ORDER BY productId, minimumBaseQuantity")
    fun observeAllPriceTiers(): Flow<List<PriceTierEntity>>

    @Insert
    suspend fun insertPriceTier(tier: PriceTierEntity): Long

    @Update
    suspend fun updatePriceTier(tier: PriceTierEntity)

    @Query("DELETE FROM price_tiers WHERE id = :id")
    suspend fun deletePriceTier(id: Long)

    @Query("SELECT * FROM stock_movements ORDER BY createdAt DESC, id DESC")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Insert
    suspend fun insertStockMovement(movement: StockMovementEntity): Long
}

@Dao
interface OperationsDao {
    @Query("SELECT * FROM parties WHERE kind = :kind ORDER BY isActive DESC, name")
    fun observeParties(kind: String): Flow<List<PartyEntity>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getParty(id: Long): PartyEntity?

    @Insert
    suspend fun insertParty(party: PartyEntity): Long

    @Update
    suspend fun updateParty(party: PartyEntity)

    @Query("SELECT * FROM purchases ORDER BY createdAt DESC, id DESC")
    fun observePurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id")
    suspend fun getPurchase(id: Long): PurchaseEntity?

    @Insert
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    @Update
    suspend fun updatePurchase(purchase: PurchaseEntity)

    @Insert
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id")
    suspend fun getPurchaseItems(purchaseId: Long): List<PurchaseItemEntity>

    @Query("SELECT * FROM cash_entries ORDER BY createdAt DESC, id DESC")
    fun observeCashEntries(): Flow<List<CashEntryEntity>>

    @Query("SELECT * FROM cash_entries WHERE createdAt BETWEEN :fromInclusive AND :toInclusive ORDER BY createdAt DESC")
    suspend fun getCashEntriesBetween(
        fromInclusive: Long,
        toInclusive: Long,
    ): List<CashEntryEntity>

    @Query("SELECT * FROM cash_entries WHERE shiftId = :shiftId ORDER BY createdAt, id")
    suspend fun getCashEntriesForShift(shiftId: Long): List<CashEntryEntity>

    @Insert
    suspend fun insertCashEntry(entry: CashEntryEntity): Long

    @Query("SELECT * FROM debts ORDER BY settlementStatus, createdAt DESC, id DESC")
    fun observeDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebt(id: Long): DebtEntity?

    @Insert
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paidAt, id")
    suspend fun getDebtPayments(debtId: Long): List<DebtPaymentEntity>

    @Insert
    suspend fun insertDebtPayment(payment: DebtPaymentEntity): Long
}

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY openedAt DESC, id DESC")
    fun observeShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC, id DESC LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE status = 'OPEN' ORDER BY openedAt DESC, id DESC LIMIT 1")
    fun observeOpenShift(): Flow<ShiftEntity?>

    @Insert
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)
}

@Dao
interface WorkforceDao {
    @Query("SELECT * FROM employees ORDER BY isActive DESC, name")
    fun observeEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployee(id: Long): EmployeeEntity?

    @Insert
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    @Query("SELECT * FROM wage_rates WHERE employeeId = :employeeId ORDER BY effectiveAt DESC")
    fun observeRates(employeeId: Long): Flow<List<WageRateEntity>>

    @Query("SELECT * FROM wage_rates WHERE employeeId = :employeeId AND effectiveAt <= :workAt ORDER BY effectiveAt DESC LIMIT 1")
    suspend fun getEffectiveRate(employeeId: Long, workAt: Long): WageRateEntity?

    @Insert
    suspend fun insertRate(rate: WageRateEntity): Long

    @Query("SELECT * FROM attendance_records ORDER BY workDate DESC, id DESC")
    fun observeAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance_records WHERE id = :id")
    suspend fun getAttendance(id: Long): AttendanceEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Query("SELECT * FROM freelance_jobs ORDER BY workDate DESC, id DESC")
    fun observeFreelanceJobs(): Flow<List<FreelanceJobEntity>>

    @Query("SELECT * FROM freelance_jobs WHERE id = :id")
    suspend fun getFreelanceJob(id: Long): FreelanceJobEntity?

    @Insert
    suspend fun insertFreelanceJob(job: FreelanceJobEntity): Long

    @Update
    suspend fun updateFreelanceJob(job: FreelanceJobEntity)

    @Query("SELECT * FROM worker_payments WHERE employeeId = :employeeId ORDER BY paidAt DESC, id DESC")
    fun observeWorkerPayments(employeeId: Long): Flow<List<WorkerPaymentEntity>>

    @Insert
    suspend fun insertWorkerPayment(payment: WorkerPaymentEntity): Long
}

@Dao
interface CulinaryDao {
    @Query("SELECT * FROM toppings ORDER BY productId, isActive DESC, label")
    fun observeAllToppings(): Flow<List<ToppingEntity>>

    @Query("SELECT * FROM toppings WHERE productId = :productId ORDER BY isActive DESC, label")
    fun observeToppings(productId: Long): Flow<List<ToppingEntity>>

    @Query("SELECT * FROM toppings WHERE productId = :productId AND isActive = 1 ORDER BY label")
    suspend fun getActiveToppings(productId: Long): List<ToppingEntity>

    @Query("SELECT * FROM toppings WHERE id = :id")
    suspend fun getTopping(id: Long): ToppingEntity?

    @Insert
    suspend fun insertTopping(topping: ToppingEntity): Long

    @Update
    suspend fun updateTopping(topping: ToppingEntity)

    @Query("SELECT * FROM recipe_ingredients WHERE menuProductId = :menuProductId")
    fun observeRecipe(menuProductId: Long): Flow<List<RecipeIngredientEntity>>

    @Query("SELECT * FROM recipe_ingredients WHERE menuProductId = :menuProductId")
    suspend fun getRecipe(menuProductId: Long): List<RecipeIngredientEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecipeIngredient(ingredient: RecipeIngredientEntity)

    @Query("DELETE FROM recipe_ingredients WHERE menuProductId = :menuProductId AND ingredientProductId = :ingredientProductId")
    suspend fun deleteRecipeIngredient(menuProductId: Long, ingredientProductId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCartLineNote(note: CartLineNoteEntity)

    @Query("SELECT * FROM cart_line_notes WHERE lineId = :lineId")
    suspend fun getCartLineNote(lineId: String): CartLineNoteEntity?

    @Query("SELECT * FROM cart_line_notes")
    fun observeCartLineNotes(): Flow<List<CartLineNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCartLineTopping(topping: CartLineToppingEntity)

    @Query("SELECT * FROM cart_line_toppings WHERE lineId = :lineId")
    suspend fun getCartLineToppings(lineId: String): List<CartLineToppingEntity>

    @Query("SELECT * FROM cart_line_toppings")
    fun observeCartLineToppings(): Flow<List<CartLineToppingEntity>>

    @Query("DELETE FROM cart_line_toppings WHERE lineId = :lineId AND toppingId = :toppingId")
    suspend fun deleteCartLineTopping(lineId: String, toppingId: Long)

    @Query("DELETE FROM cart_line_notes WHERE lineId = :lineId")
    suspend fun deleteCartLineNote(lineId: String)

    @Query("DELETE FROM cart_line_toppings WHERE lineId = :lineId")
    suspend fun deleteCartLineToppingsByLine(lineId: String)

    @Query("DELETE FROM cart_line_notes")
    suspend fun clearCartLineNotes()

    @Query("DELETE FROM cart_line_toppings")
    suspend fun clearCartLineToppings()

    @Insert
    suspend fun insertSaleItemToppings(items: List<SaleItemToppingEntity>)

    @Query("SELECT * FROM sale_item_toppings WHERE saleItemId = :saleItemId ORDER BY id")
    suspend fun getSaleItemToppings(saleItemId: Long): List<SaleItemToppingEntity>
}

@Dao
interface SecurityDao {
    @Query("SELECT * FROM report_security WHERE id = 1")
    suspend fun getReportSecurity(): ReportSecurityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveReportSecurity(security: ReportSecurityEntity)
}

data class SalesAggregate(
    val transactionCount: Int,
    val totalSales: Long,
)

data class PaymentAggregate(
    val paymentMethod: String,
    val total: Long,
)

data class CashAggregate(
    val type: String,
    val total: Long,
)

data class ForecastSalesRow(
    val productId: Long,
    val productName: String,
    val unitLabel: String,
    val quantity: Int,
    val baseQuantity: Int,
    val createdAt: Long,
)

@Dao
interface ReportDao {
    @Query(
        """
        SELECT sale_items.productId AS productId,
               sale_items.productName AS productName,
               sale_items.unitLabel AS unitLabel,
               sale_items.quantity AS quantity,
               sale_items.baseQuantity AS baseQuantity,
               sales.createdAt AS createdAt
        FROM sale_items
        INNER JOIN sales ON sales.id = sale_items.saleId
        WHERE sales.createdAt BETWEEN :fromInclusive AND :toInclusive
          AND sales.orderStatus IN ('COMPLETED', 'NEW', 'PROCESSING', 'READY')
        ORDER BY sales.createdAt, sale_items.id
        """,
    )
    suspend fun forecastSales(
        fromInclusive: Long,
        toInclusive: Long,
    ): List<ForecastSalesRow>

    @Query(
        """
        SELECT COUNT(*) AS transactionCount, COALESCE(SUM(total), 0) AS totalSales
        FROM sales
        WHERE createdAt BETWEEN :fromInclusive AND :toInclusive
        """,
    )
    suspend fun salesSummary(fromInclusive: Long, toInclusive: Long): SalesAggregate

    @Query(
        """
        SELECT paymentMethod, COALESCE(SUM(
            CASE WHEN paymentMethod = 'CASH' THEN total ELSE amountReceived END
        ), 0) AS total
        FROM sales
        WHERE createdAt BETWEEN :fromInclusive AND :toInclusive
        GROUP BY paymentMethod
        ORDER BY paymentMethod
        """,
    )
    suspend fun paymentSummary(
        fromInclusive: Long,
        toInclusive: Long,
    ): List<PaymentAggregate>

    @Query(
        """
        SELECT type, COALESCE(SUM(amount), 0) AS total
        FROM cash_entries
        WHERE createdAt BETWEEN :fromInclusive AND :toInclusive
        GROUP BY type
        ORDER BY type
        """,
    )
    suspend fun cashSummary(
        fromInclusive: Long,
        toInclusive: Long,
    ): List<CashAggregate>

    @Query(
        """
        SELECT COALESCE(SUM(originalAmount - paidAmount), 0)
        FROM debts
        WHERE kind = :kind AND settlementStatus != 'PAID'
        """,
    )
    suspend fun outstandingDebt(kind: String): Long
}
