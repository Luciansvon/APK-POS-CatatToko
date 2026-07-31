package com.bimacore.usahakecil.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bimacore.usahakecil.PosApplication
import com.bimacore.usahakecil.backup.BackupManager
import com.bimacore.usahakecil.backup.BackupPreview
import com.bimacore.usahakecil.data.BusinessProfileEntity
import com.bimacore.usahakecil.data.CategoryDraft
import com.bimacore.usahakecil.data.CulinaryRepository
import com.bimacore.usahakecil.data.DebtEntity
import com.bimacore.usahakecil.data.DebtKind
import com.bimacore.usahakecil.data.EmployeeEntity
import com.bimacore.usahakecil.data.InventoryRepository
import com.bimacore.usahakecil.data.ManualCashType
import com.bimacore.usahakecil.data.OperationsRepository
import com.bimacore.usahakecil.data.PartyKind
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.data.ProductDraft
import com.bimacore.usahakecil.data.PurchaseDraft
import com.bimacore.usahakecil.data.PurchaseLineDraft
import com.bimacore.usahakecil.data.ReportRepository
import com.bimacore.usahakecil.data.ReportSummary
import com.bimacore.usahakecil.data.SaleEntity
import com.bimacore.usahakecil.data.SaleItemEntity
import com.bimacore.usahakecil.data.VariantDraft
import com.bimacore.usahakecil.data.WorkerScheme
import com.bimacore.usahakecil.data.WorkforceRepository
import com.bimacore.usahakecil.domain.AttendanceStatus
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.OrderStatus
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaleDetail(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>,
)

class OperationsViewModel(
    val capabilities: BusinessCapabilities,
    private val database: PosDatabase,
    private val inventory: InventoryRepository,
    private val operations: OperationsRepository,
    private val workforce: WorkforceRepository,
    private val reports: ReportRepository,
    private val culinary: CulinaryRepository,
    private val backups: BackupManager,
) : ViewModel() {
    val profile = database.profileDao().observeProfile().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
    val categories = inventory.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val products = inventory.products.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val variants = inventory.variants.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val stockMovements = inventory.stockMovements.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val suppliers = operations.suppliers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val customers = operations.customers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val purchases = operations.purchases.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val cashEntries = operations.cashEntries.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val debts = operations.debts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val employees = workforce.employees.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val attendance = workforce.attendance.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val freelanceJobs = workforce.freelanceJobs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val sales = database.saleDao().observeSales().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val openOrders = culinary.openOrders.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _reportSummary = MutableStateFlow<ReportSummary?>(null)
    val reportSummary = _reportSummary.asStateFlow()
    private val _reportHasPin = MutableStateFlow<Boolean?>(null)
    val reportHasPin = _reportHasPin.asStateFlow()
    val ownerUnlocked = reports.session.unlocked
    private val _backupUri = MutableStateFlow<Uri?>(null)
    val backupUri = _backupUri.asStateFlow()
    private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
    val backupPreview = _backupPreview.asStateFlow()
    private val _restoreCompleted = MutableStateFlow(false)
    val restoreCompleted = _restoreCompleted.asStateFlow()
    private val _saleDetail = MutableStateFlow<SaleDetail?>(null)
    val saleDetail = _saleDetail.asStateFlow()

    init {
        refreshPinState()
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun saveProfile(name: String) = execute("Profil usaha disimpan") {
        require(name.isNotBlank()) { "Nama usaha wajib diisi" }
        val current = requireNotNull(database.profileDao().getProfile()) {
            "Profil usaha belum tersedia"
        }
        database.profileDao().saveProfile(
            current.copy(
                businessName = name.trim(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    fun saveCategory(
        id: Long?,
        name: String,
    ) = execute(if (id == null) "Kategori ditambahkan" else "Kategori diperbarui") {
        inventory.saveCategory(CategoryDraft(id = id, name = name))
    }

    fun saveProduct(
        id: Long?,
        categoryId: Long,
        name: String,
        price: Long,
        stock: Int,
        unit: String,
    ) = execute(if (id == null) "Produk ditambahkan" else "Produk diperbarui") {
        val current = id?.let { productId ->
            products.value.firstOrNull { it.id == productId }
        }
        inventory.saveProduct(
            ProductDraft(
                id = id,
                categoryId = categoryId,
                name = name,
                basePrice = price,
                openingStock = stock,
                stockTrackingEnabled = current?.stockTrackingEnabled ?: true,
                lowStockThreshold = current?.lowStockThreshold ?: 5,
                unitLabel = unit,
            ),
        )
    }

    fun saveVariant(
        productId: Long,
        label: String,
        priceOverride: Long?,
        stock: Int,
    ) = execute("Varian ditambahkan") {
        inventory.saveVariant(
            VariantDraft(
                productId = productId,
                label = label,
                priceOverride = priceOverride,
                openingStock = stock,
            ),
        )
    }

    fun setProductActive(
        productId: Long,
        active: Boolean,
    ) = execute(if (active) "Produk diaktifkan" else "Produk dinonaktifkan") {
        inventory.setProductActive(productId, active)
    }

    fun setVariantActive(
        variantId: Long,
        active: Boolean,
    ) = execute(if (active) "Varian diaktifkan" else "Varian dinonaktifkan") {
        inventory.setVariantActive(variantId, active)
    }

    fun adjustStock(
        productId: Long,
        variantId: Long?,
        delta: Int,
        type: String,
        reason: String,
    ) = execute("Stok diperbarui") {
        inventory.adjustStock(productId, variantId, delta, type, reason)
    }

    fun saveUnit(
        productId: Long,
        label: String,
        factor: Int,
        price: Long,
    ) = execute("Satuan grosir ditambahkan") {
        inventory.saveUnit(null, productId, label, factor, price)
    }

    fun savePriceTier(
        productId: Long,
        minimumQuantity: Int,
        unitPrice: Long,
    ) = execute("Harga bertingkat ditambahkan") {
        inventory.savePriceTier(null, productId, minimumQuantity, unitPrice)
    }

    fun saveParty(
        kind: PartyKind,
        name: String,
        phone: String,
        address: String,
    ) = execute(if (kind == PartyKind.SUPPLIER) "Supplier ditambahkan" else "Pelanggan ditambahkan") {
        require(kind != PartyKind.CUSTOMER || capabilities.customerReceivables) {
            "Pelanggan dan piutang tidak aktif pada APK ini"
        }
        operations.saveParty(null, kind, name, phone, address)
    }

    fun recordPurchase(
        supplierId: Long,
        productId: Long,
        variantId: Long?,
        quantity: Int,
        unitCost: Long,
        amountPaid: Long,
        note: String,
    ) = execute("Pembelian disimpan dan stok bertambah") {
        val product = requireNotNull(products.value.firstOrNull { it.id == productId }) {
            "Produk tidak tersedia"
        }
        operations.recordPurchase(
            PurchaseDraft(
                supplierId = supplierId,
                amountPaid = amountPaid,
                note = note,
                lines = listOf(
                    PurchaseLineDraft(
                        productId = productId,
                        variantId = variantId,
                        unitLabel = product.unitLabel,
                        quantity = quantity,
                        unitCost = unitCost,
                    ),
                ),
            ),
        )
    }

    fun addCash(
        type: ManualCashType,
        amount: Long,
        category: String,
        note: String,
    ) = execute("Catatan kas disimpan") {
        operations.addManualCashEntry(type, amount, category, note)
    }

    fun createDebt(
        kind: DebtKind,
        partyId: Long,
        amount: Long,
        initialPayment: Long,
        note: String,
    ) = execute("Tagihan disimpan") {
        require(kind != DebtKind.RECEIVABLE || capabilities.customerReceivables) {
            "Piutang pelanggan tidak aktif pada APK ini"
        }
        operations.createDebt(kind, partyId, amount, initialPayment, note)
    }

    fun payDebt(
        debt: DebtEntity,
        amount: Long,
    ) = execute("Pembayaran tagihan disimpan") {
        operations.payDebt(debt.id, amount, "CASH", "Pembayaran dari aplikasi")
    }

    fun saveEmployee(
        name: String,
        phone: String,
        scheme: WorkerScheme,
        dailyRate: Long?,
    ) = execute("Pekerja ditambahkan") {
        workforce.saveEmployeeWithInitialRate(
            name = name,
            phone = phone,
            scheme = scheme,
            dailyRate = dailyRate,
            effectiveAt = startOfToday(),
        )
    }

    fun recordAttendance(
        employee: EmployeeEntity,
        status: AttendanceStatus,
        overtime: Long,
        bonus: Long,
        deduction: Long,
        advance: Long,
    ) = execute("Kehadiran disimpan") {
        workforce.recordAttendance(
            employeeId = employee.id,
            workDate = startOfToday(),
            status = status,
            overtime = overtime,
            bonus = bonus,
            deduction = deduction,
            advance = advance,
            note = "",
        )
    }

    fun payAttendance(id: Long) = execute("Upah harian dibayar") {
        workforce.payAttendance(id, "Pembayaran upah harian")
    }

    fun updateDailyRate(
        employeeId: Long,
        rate: Long,
    ) = execute("Tarif harian baru disimpan") {
        workforce.addDailyRate(employeeId, rate, startOfToday())
    }

    fun createFreelanceJob(
        employee: EmployeeEntity,
        title: String,
        amount: Long,
    ) = execute("Pekerjaan freelancer disimpan") {
        workforce.createFreelanceJob(
            employee.id,
            title,
            amount,
            startOfToday(),
            "",
        )
    }

    fun payFreelanceJob(
        jobId: Long,
        amount: Long,
    ) = execute("Cicilan freelancer disimpan") {
        workforce.payFreelanceJob(jobId, amount, "Pembayaran pekerjaan")
    }

    fun createReportPin(pin: String) = execute("PIN Owner dibuat. Mode Owner aktif.") {
        reports.createPin(pin)
        _reportHasPin.value = true
        loadReport()
    }

    fun unlockReport(pin: String) = execute {
        if (!reports.unlock(pin)) {
            throw IllegalArgumentException("PIN Owner salah")
        }
        loadReport()
        _message.value = "Mode Owner aktif"
    }

    fun lockReport() {
        reports.lock()
        _reportSummary.value = null
        _message.value = "Mode Owner dikunci"
    }

    fun changeReportPin(
        currentPin: String,
        newPin: String,
    ) = execute("PIN Owner berhasil diganti") {
        reports.changePin(currentPin, newPin)
    }

    fun refreshReport() = execute {
        loadReport()
    }

    fun openSaleDetail(sale: SaleEntity) = execute {
        _saleDetail.value = SaleDetail(
            sale = sale,
            items = database.saleDao().getItems(sale.id),
        )
    }

    fun closeSaleDetail() {
        _saleDetail.value = null
    }

    fun beginRestoreFileSelection() {
        reports.session.beginExternalOwnerFlow()
    }

    fun finishRestoreFileSelection(uri: Uri?) {
        reports.session.endExternalOwnerFlow()
        uri?.let(::inspectBackup)
    }

    fun createBackup() = execute("Backup siap dibagikan") {
        reports.session.requireOwner()
        _backupUri.value = backups.createBackup()
    }

    fun inspectBackup(uri: Uri) = execute {
        reports.session.requireOwner()
        _backupPreview.value = backups.preview(uri)
        _message.value = "Backup valid. Periksa identitas sebelum restore."
    }

    fun cancelRestore() {
        _backupPreview.value = null
    }

    fun confirmRestore() = execute("Restore selesai") {
        reports.session.requireOwner()
        val preview = requireNotNull(_backupPreview.value) { "Pilih file backup dulu" }
        backups.restore(preview)
        _backupPreview.value = null
        _restoreCompleted.value = true
    }

    fun saveTopping(
        productId: Long,
        label: String,
        price: Long,
    ) = execute("Topping ditambahkan") {
        culinary.saveTopping(null, productId, label, price)
    }

    fun saveRecipe(
        menuProductId: Long,
        ingredientProductId: Long,
        quantity: Int,
    ) = execute("Resep bahan disimpan") {
        culinary.saveRecipeIngredient(menuProductId, ingredientProductId, quantity)
    }

    fun moveOrder(
        saleId: Long,
        status: OrderStatus,
    ) = execute("Status pesanan diperbarui") {
        culinary.moveOrder(saleId, status)
    }

    private fun refreshPinState() {
        viewModelScope.launch {
            _reportHasPin.value = reports.hasPin()
        }
    }

    private suspend fun loadReport() {
        val now = System.currentTimeMillis()
        _reportSummary.value = reports.readSummary(startOfToday(), now)
    }

    private fun execute(
        successMessage: String? = null,
        action: suspend () -> Unit,
    ) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            try {
                action()
                if (successMessage != null) _message.value = successMessage
            } catch (error: Exception) {
                _message.value = error.message ?: "Data gagal disimpan"
            } finally {
                _busy.value = false
            }
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().run {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    class Factory(
        private val application: PosApplication,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            OperationsViewModel(
                capabilities = application.capabilities,
                database = application.database,
                inventory = application.newInventoryRepository(),
                operations = application.newOperationsRepository(),
                workforce = application.newWorkforceRepository(),
                reports = application.newReportRepository(),
                culinary = application.newCulinaryRepository(),
                backups = application.newBackupManager(),
            ) as T
    }
}
