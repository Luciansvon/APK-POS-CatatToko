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
import com.bimacore.usahakecil.data.ProductForecastReport
import com.bimacore.usahakecil.data.PurchaseDraft
import com.bimacore.usahakecil.data.PurchaseLineDraft
import com.bimacore.usahakecil.data.ReportChartGranularity
import com.bimacore.usahakecil.data.ReportChartMode
import com.bimacore.usahakecil.data.ReportPeriod
import com.bimacore.usahakecil.data.ReportProductMeasure
import com.bimacore.usahakecil.data.ReportRepository
import com.bimacore.usahakecil.data.ReportSummary
import com.bimacore.usahakecil.data.ReportTrendReport
import com.bimacore.usahakecil.data.SaleEntity
import com.bimacore.usahakecil.data.SaleItemEntity
import com.bimacore.usahakecil.data.ShiftSummary
import com.bimacore.usahakecil.data.VariantDraft
import com.bimacore.usahakecil.data.WorkerScheme
import com.bimacore.usahakecil.data.WorkforceRepository
import com.bimacore.usahakecil.domain.AttendanceStatus
import com.bimacore.usahakecil.domain.BusinessCapabilities
import com.bimacore.usahakecil.domain.OrderStatus
import com.bimacore.usahakecil.export.ExcelExportManager
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaleDetail(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>,
)

class OperationsViewModel(
    private val application: PosApplication,
    val capabilities: BusinessCapabilities,
    private val database: PosDatabase,
    private val inventory: InventoryRepository,
    private val operations: OperationsRepository,
    private val workforce: WorkforceRepository,
    private val reports: ReportRepository,
    private val culinary: CulinaryRepository,
    private val backups: BackupManager,
    private val excelExports: ExcelExportManager,
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
    val shifts = operations.shifts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )
    val openShift = operations.openShift.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
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
    private val _previousReportSummary = MutableStateFlow<ReportSummary?>(null)
    val previousReportSummary = _previousReportSummary.asStateFlow()
    private val _reportPeriod = MutableStateFlow(ReportPeriod.DAY)
    val reportPeriod = _reportPeriod.asStateFlow()
    private val _reportChartMode = MutableStateFlow(ReportChartMode.CASH_FLOW)
    val reportChartMode = _reportChartMode.asStateFlow()
    private val _reportChartGranularity = MutableStateFlow(ReportChartGranularity.DAILY)
    val reportChartGranularity = _reportChartGranularity.asStateFlow()
    private val _reportProductMeasure = MutableStateFlow(ReportProductMeasure.SALES)
    val reportProductMeasure = _reportProductMeasure.asStateFlow()
    private val _selectedReportProductId = MutableStateFlow<Long?>(null)
    val selectedReportProductId = _selectedReportProductId.asStateFlow()
    private val _reportTrend = MutableStateFlow<ReportTrendReport?>(null)
    val reportTrend = _reportTrend.asStateFlow()
    private val _reportTrendError = MutableStateFlow<String?>(null)
    val reportTrendError = _reportTrendError.asStateFlow()
    private val _forecastReport = MutableStateFlow<ProductForecastReport?>(null)
    val forecastReport = _forecastReport.asStateFlow()
    private val _forecastLoading = MutableStateFlow(false)
    val forecastLoading = _forecastLoading.asStateFlow()
    private val _forecastError = MutableStateFlow<String?>(null)
    val forecastError = _forecastError.asStateFlow()
    private val _reportHasPin = MutableStateFlow<Boolean?>(null)
    val reportHasPin = _reportHasPin.asStateFlow()
    val ownerUnlocked = reports.session.unlocked
    private val _backupUri = MutableStateFlow<Uri?>(null)
    val backupUri = _backupUri.asStateFlow()
    private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
    val backupPreview = _backupPreview.asStateFlow()
    private val _excelUri = MutableStateFlow<Uri?>(null)
    val excelUri = _excelUri.asStateFlow()
    private val _excelError = MutableStateFlow<String?>(null)
    val excelError = _excelError.asStateFlow()
    private val _restoreCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restoreCompleted = _restoreCompleted.asSharedFlow()
    private val _pendingRestoreUri = MutableStateFlow<Uri?>(null)
    private val _saleDetail = MutableStateFlow<SaleDetail?>(null)
    val saleDetail = _saleDetail.asStateFlow()
    private val _shiftSummary = MutableStateFlow<ShiftSummary?>(null)
    val shiftSummary = _shiftSummary.asStateFlow()
    private val _shiftLoading = MutableStateFlow(false)
    val shiftLoading = _shiftLoading.asStateFlow()

    private var reportJob: Job? = null
    private val _reportLoading = MutableStateFlow(false)
    val reportLoading = _reportLoading.asStateFlow()

    init {
        refreshPinState()
        refreshShiftSummary()
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
        imageUri: String?,
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
                imageUri = imageUri,
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
    ) = execute(if (kind == PartyKind.SUPPLIER) "Pemasok ditambahkan" else "Pelanggan ditambahkan") {
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

    fun openShift(
        cashierName: String,
        openingCash: Long,
        openingNote: String,
    ) = execute("Shift dibuka") {
        operations.openShift(cashierName, openingCash, openingNote)
        loadShiftSummary()
    }

    fun closeShift(
        closingCash: Long,
        closingNote: String,
    ) = execute("Shift ditutup") {
        operations.closeShift(closingCash, closingNote)
        _shiftSummary.value = null
    }

    fun refreshShiftSummary() {
        if (!reports.session.isUnlocked) {
            _shiftSummary.value = null
            return
        }
        if (_shiftLoading.value) return
        viewModelScope.launch { loadShiftSummary() }
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
    ) = execute("Pekerjaan panggilan disimpan") {
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
    ) = execute("Pembayaran pekerja panggilan disimpan") {
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
        val pendingUri = _pendingRestoreUri.value
        if (pendingUri != null) {
            _backupPreview.value = backups.preview(pendingUri)
            _pendingRestoreUri.value = null
            _message.value = "Salinan valid. Periksa identitas sebelum memulihkan data."
        } else {
            _message.value = "Mode Owner aktif"
        }
    }

    fun lockReport() {
        reports.lock()
        _reportSummary.value = null
        _previousReportSummary.value = null
        _reportPeriod.value = ReportPeriod.DAY
        _reportChartMode.value = ReportChartMode.CASH_FLOW
        _reportChartGranularity.value = ReportChartGranularity.DAILY
        _reportProductMeasure.value = ReportProductMeasure.SALES
        _selectedReportProductId.value = null
        _reportTrend.value = null
        _reportTrendError.value = null
        _excelUri.value = null
        _excelError.value = null
        _forecastReport.value = null
        _forecastError.value = null
        _message.value = "Mode Owner dikunci"
    }

    fun changeReportPin(
        currentPin: String,
        newPin: String,
    ) = execute("PIN Owner berhasil diganti") {
        reports.changePin(currentPin, newPin)
    }

    fun refreshReport() = executeReport {
        loadReport()
    }

    fun selectReportPeriod(period: ReportPeriod) {
        if (_reportPeriod.value == period) return
        _reportPeriod.value = period
        _reportChartGranularity.value = when (period) {
            ReportPeriod.DAY,
            ReportPeriod.WEEK,
            -> ReportChartGranularity.DAILY
            ReportPeriod.MONTH -> ReportChartGranularity.WEEKLY
            ReportPeriod.YEAR -> ReportChartGranularity.MONTHLY
        }
        _excelUri.value = null
        _excelError.value = null
        if (reports.session.isUnlocked) {
            executeReport { loadReport() }
        }
    }

    fun selectReportChartMode(mode: ReportChartMode) {
        _reportChartMode.value = mode
    }

    fun selectReportChartGranularity(granularity: ReportChartGranularity) {
        if (_reportChartGranularity.value == granularity) return
        _reportChartGranularity.value = granularity
        if (reports.session.isUnlocked) {
            executeReport { loadReportTrend() }
        }
    }

    fun selectReportProduct(productId: Long?) {
        _selectedReportProductId.value = productId
    }

    fun selectReportProductMeasure(measure: ReportProductMeasure) {
        _reportProductMeasure.value = measure
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
        if (uri == null) {
            _pendingRestoreUri.value = null
            return
        }
        _pendingRestoreUri.value = uri
        _message.value = "Berkas dipilih. Masukkan PIN Owner lagi untuk memeriksa."
    }

    fun createBackup() = execute("Salinan data siap dibagikan") {
        reports.session.requireOwner()
        _backupUri.value = backups.createBackup()
    }

    fun createExcelExport() {
        execute {
            reports.session.requireOwner()
            val period = _reportPeriod.value
            _excelError.value = null
            try {
                _excelUri.value = excelExports.createExport(period)
                _message.value = "Excel ${period.label} siap dibagikan"
            } catch (error: Exception) {
                _excelError.value = error.message ?: "File Excel gagal dibuat"
                throw error
            }
        }
    }

    fun inspectBackup(uri: Uri) = execute {
        reports.session.requireOwner()
        _backupPreview.value = backups.preview(uri)
        _message.value = "Salinan valid. Periksa identitas sebelum memulihkan data."
    }

    fun cancelRestore() {
        _backupPreview.value = null
    }

    fun confirmRestore() = execute("Pemulihan selesai") {
        reports.session.requireOwner()
        val preview = requireNotNull(_backupPreview.value) { "Pilih berkas salinan dulu" }
        backups.restore(preview)
        _backupPreview.value = null
        _restoreCompleted.tryEmit(Unit)
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
            val hasPin = reports.hasPin()
            _reportHasPin.value = hasPin
            if (hasPin && reports.session.isUnlocked) {
                loadReport()
            }
        }
    }

    private suspend fun loadReport() {
        val now = System.currentTimeMillis()
        val range = _reportPeriod.value.range(now)
        _reportSummary.value = reports.readSummary(range.first, range.last)
        val previousRange = _reportPeriod.value.previousRange(now)
        _previousReportSummary.value = reports.readSummary(previousRange.first, previousRange.last)
        loadReportTrend()
    }

    fun loadProductForecasts() = executeReport {
        val now = System.currentTimeMillis()
        _forecastLoading.value = true
        _forecastError.value = null
        try {
            _forecastReport.value = reports.readProductForecasts(toInclusive = now)
        } catch (_: Exception) {
            _forecastReport.value = null
            _forecastError.value = "Prediksi belum dapat dimuat sekarang"
        } finally {
            _forecastLoading.value = false
        }
    }

    private suspend fun loadReportTrend() {
        _reportTrendError.value = null
        try {
            _reportTrend.value = reports.readTrend(_reportChartGranularity.value)
        } catch (_: Exception) {
            _reportTrend.value = null
            _reportTrendError.value = "Grafik belum dapat dimuat sekarang"
        }
    }

    private suspend fun loadShiftSummary() {
        if (!reports.session.isUnlocked) {
            _shiftSummary.value = null
            return
        }
        _shiftLoading.value = true
        try {
            _shiftSummary.value = operations.readOpenShiftSummary()
        } finally {
            _shiftLoading.value = false
        }
    }

    private fun executeReport(action: suspend () -> Unit) {
        reportJob?.cancel()
        reportJob = viewModelScope.launch {
            _reportLoading.value = true
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _message.value = error.message ?: "Laporan gagal dimuat"
            } finally {
                _reportLoading.value = false
            }
        }
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
                application = application,
                capabilities = application.capabilities,
                database = application.database,
                inventory = application.newInventoryRepository(),
                operations = application.newOperationsRepository(),
                workforce = application.newWorkforceRepository(),
                reports = application.newReportRepository(),
                culinary = application.newCulinaryRepository(),
                backups = application.newBackupManager(),
                excelExports = application.newExcelExportManager(),
            ) as T
    }

}
