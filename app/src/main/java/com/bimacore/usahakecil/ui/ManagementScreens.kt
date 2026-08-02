package com.bimacore.usahakecil.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableView
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.data.DebtEntity
import com.bimacore.usahakecil.data.DebtKind
import com.bimacore.usahakecil.data.EmployeeEntity
import com.bimacore.usahakecil.data.ManualCashType
import com.bimacore.usahakecil.data.PartyKind
import com.bimacore.usahakecil.data.ProductEntity
import com.bimacore.usahakecil.data.ReportPeriod
import com.bimacore.usahakecil.data.WorkerScheme
import com.bimacore.usahakecil.domain.AttendanceStatus
import com.bimacore.usahakecil.domain.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun ownerTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.primary,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperationsScreen(
    viewModel: OperationsViewModel,
    startSection: String = "Produk",
    title: String = "Operasional",
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val variants by viewModel.variants.collectAsState()
    val movements by viewModel.stockMovements.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val jobs by viewModel.freelanceJobs.collectAsState()
    val orders by viewModel.openOrders.collectAsState()
    val tabs = buildList {
        add("Produk")
        add("Stok")
        add("Pembelian")
        add("Pekerja")
        if (viewModel.capabilities.multiUnit) add("Grosir")
        if (viewModel.capabilities.culinaryOrders) add("Kuliner")
    }
    var tab by remember(startSection, tabs) {
        mutableIntStateOf(tabs.indexOf(startSection).coerceAtLeast(0))
    }
    var dialog by remember { mutableStateOf<String?>(null) }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }
    var selectedJobId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                colors = ownerTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OwnerSectionTabs(
                items = tabs,
                selectedIndex = tab,
                onSelected = { tab = it },
                testTag = "operations-section-grid",
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tabs[tab]) {
                    "Produk" -> {
                        ActionRow(
                            "Tambah kategori" to {
                                selectedCategoryId = null
                                dialog = "category"
                            },
                            "Tambah produk" to {
                                selectedProductId = null
                                dialog = "product"
                            },
                            "Tambah varian" to { dialog = "variant" },
                        )
                        CategoryGrid(
                            categories = categories,
                            onEdit = { category ->
                                selectedCategoryId = category.id
                                dialog = "category"
                            },
                        )
                        products.forEach { product ->
                            val displayStock = if (product.hasVariants) {
                                variants.filter { it.productId == product.id }.sumOf { it.stock }
                            } else {
                                product.stock
                            }
                            ItemCard(
                                title = product.name,
                                subtitle = "${formatRupiah(product.basePrice)} • stok $displayStock ${product.unitLabel}",
                                action = if (product.isActive) "Nonaktifkan" else "Aktifkan",
                                onAction = {
                                    viewModel.setProductActive(product.id, !product.isActive)
                                },
                                secondaryAction = "Ubah",
                                onSecondaryAction = {
                                    selectedProductId = product.id
                                    dialog = "product"
                                },
                            )
                        }
                        variants.forEach { variant ->
                            val productName = products.firstOrNull { it.id == variant.productId }?.name
                                ?: "Produk tidak tersedia"
                            ItemCard(
                                title = "${productName} • ${variant.label}",
                                subtitle = variant.priceOverride?.let(::formatRupiah)
                                    ?: "Mengikuti harga produk",
                                action = if (variant.isActive) "Nonaktifkan" else "Aktifkan",
                            ) {
                                viewModel.setVariantActive(variant.id, !variant.isActive)
                            }
                        }
                    }
                    "Stok" -> {
                        val stockItems = products.filter { it.isActive }.map { product ->
                            val displayStock = if (product.hasVariants) {
                                variants.filter { it.productId == product.id && it.isActive }.sumOf { it.stock }
                            } else {
                                product.stock
                            }
                            product to displayStock
                        }
                        val outOfStock = stockItems.count { (product, stock) ->
                            product.stockTrackingEnabled && stock <= 0
                        }
                        val lowStock = stockItems.count { (product, stock) ->
                            product.stockTrackingEnabled && stock in 1..product.lowStockThreshold
                        }
                        OwnerHeroCard(
                            eyebrow = "Stok perlu perhatian",
                            value = "${outOfStock + lowStock} produk",
                            supportingText = if (outOfStock + lowStock == 0) {
                                "Semua stok yang dilacak masih aman."
                            } else {
                                "Dahulukan barang habis dan hampir habis."
                            },
                        ) {
                            OwnerMetricStrip(
                                listOf(
                                    outOfStock.toString() to "Habis",
                                    lowStock.toString() to "Menipis",
                                    stockItems.size.toString() to "Produk aktif",
                                ),
                            )
                        }
                        SectionTitle("Daftar stok")
                        if (stockItems.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada produk aktif",
                                message = "Tambahkan produk dulu sebelum mencatat stok masuk atau keluar.",
                                actionLabel = "Buka Produk",
                                onAction = { tab = tabs.indexOf("Produk") },
                                testTag = "stock-empty-state",
                            )
                        } else {
                            stockItems.sortedWith(
                                compareBy<Pair<ProductEntity, Int>> {
                                    when {
                                        !it.first.stockTrackingEnabled -> 3
                                        it.second <= 0 -> 0
                                        it.second <= it.first.lowStockThreshold -> 1
                                        else -> 2
                                    }
                                }.thenBy { it.first.name },
                            ).forEach { (product, displayStock) ->
                                val status = when {
                                    !product.stockTrackingEnabled -> "Stok tidak dilacak"
                                    displayStock <= 0 -> "Stok habis"
                                    displayStock <= product.lowStockThreshold -> "Stok menipis"
                                    else -> "Stok aman"
                                }
                                OwnerDetailCard(
                                    title = product.name,
                                    subtitle = "$displayStock ${product.unitLabel} • $status",
                                    action = "Atur stok",
                                ) {
                                    selectedProductId = product.id
                                    dialog = "stock"
                                }
                            }
                        }
                        SectionTitle("Riwayat terbaru")
                        if (movements.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada perubahan stok",
                                message = "Riwayat akan muncul setelah stok masuk, keluar, rusak, atau hilang dicatat.",
                                testTag = "stock-history-empty-state",
                            )
                        } else {
                            movements.take(30).forEach {
                                OwnerDetailCard(
                                    "${it.type}: ${it.baseQuantityDelta}",
                                    "${it.reason} • ${formatDate(it.createdAt)}",
                                )
                            }
                        }
                    }
                    "Pembelian" -> {
                        OwnerHeroCard(
                            eyebrow = "Total pembelian tercatat",
                            value = formatRupiah(purchases.sumOf { it.total }),
                            supportingText = "${purchases.size} pembelian dari ${suppliers.size} pemasok.",
                        )
                        Button(
                            onClick = { dialog = "purchase" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) { Text("Catat pembelian") }
                        OutlinedButton(
                            onClick = { dialog = "supplier" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Tambah pemasok") }
                        SectionTitle("Riwayat pembelian")
                        if (purchases.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada pembelian",
                                message = "Catat belanja dari pemasok agar biaya dan stok punya riwayat yang jelas.",
                                actionLabel = "Catat pembelian",
                                onAction = { dialog = "purchase" },
                                testTag = "purchase-empty-state",
                            )
                        } else {
                            purchases.forEach {
                                OwnerDetailCard(
                                    it.supplierName,
                                    "${formatRupiah(it.total)} • ${settlementLabel(it.settlementStatus)}",
                                )
                            }
                        }
                        SectionTitle("Pemasok")
                        if (suppliers.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada pemasok",
                                message = "Pemasok bisa ditambahkan sekarang atau saat mencatat pembelian.",
                            )
                        } else {
                            suppliers.forEach {
                                OwnerDetailCard(it.name, it.phone.ifBlank { "Nomor telepon belum diisi" })
                            }
                        }
                    }
                    "Pekerja" -> {
                        val unpaidAttendance = attendance.filter { !it.isPaid }
                        val unpaidJobs = jobs.filter { it.paidAmount < it.agreedAmount }
                        OwnerHeroCard(
                            eyebrow = "Pembayaran pekerja tertunda",
                            value = "${unpaidAttendance.size + unpaidJobs.size} catatan",
                            supportingText = if (unpaidAttendance.isEmpty() && unpaidJobs.isEmpty()) {
                                "Belum ada upah atau pekerjaan yang perlu dibayar."
                            } else {
                                "Periksa upah harian dan pekerjaan panggilan yang belum lunas."
                            },
                        ) {
                            OwnerMetricStrip(
                                listOf(
                                    employees.size.toString() to "Pekerja",
                                    unpaidAttendance.size.toString() to "Upah tertunda",
                                    unpaidJobs.size.toString() to "Kerja tertunda",
                                ),
                            )
                        }
                        Button(
                            onClick = { dialog = "worker" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) { Text("Tambah pekerja") }
                        SectionTitle("Daftar pekerja")
                        if (employees.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada pekerja",
                                message = "Tambahkan pekerja harian atau pekerja panggilan agar catatan kerja dan pembayaran tidak tercecer.",
                                actionLabel = "Tambah pekerja",
                                onAction = { dialog = "worker" },
                                testTag = "worker-empty-state",
                            )
                        } else {
                            employees.forEach { employee ->
                                OwnerDetailCard(
                                    title = employee.name,
                                    subtitle = if (employee.scheme == "DAILY") "Pekerja harian" else "Pekerja panggilan",
                                    action = if (employee.scheme == "DAILY") "Catat hadir" else "Tambah kerja",
                                ) {
                                    selectedEmployee = employee
                                    dialog = if (employee.scheme == "DAILY") "attendance" else "job"
                                }
                                if (employee.scheme == "DAILY") {
                                    TextButton(onClick = {
                                        selectedEmployee = employee
                                        dialog = "rate"
                                    }) { Text("Ubah tarif ${employee.name}") }
                                }
                            }
                        }
                        SectionTitle("Kehadiran belum dibayar")
                        if (unpaidAttendance.isEmpty()) {
                            Text(
                                "Tidak ada upah harian yang tertunda.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else unpaidAttendance.forEach {
                            OwnerDetailCard(
                                "Upah ${formatRupiah(it.netPay)}",
                                "${it.status} • ${formatDate(it.workDate)}",
                                "Bayar",
                            ) { viewModel.payAttendance(it.id) }
                        }
                        SectionTitle("Pekerjaan panggilan")
                        if (jobs.isEmpty()) {
                            Text(
                                "Belum ada pekerjaan panggilan.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else jobs.forEach {
                            OwnerDetailCard(
                                it.title,
                                "${formatRupiah(it.paidAmount)} / ${formatRupiah(it.agreedAmount)}",
                                if (it.paidAmount < it.agreedAmount) "Bayar/cicil" else null,
                            ) {
                                selectedJobId = it.id
                                dialog = "pay_job"
                            }
                        }
                    }
                    "Grosir" -> {
                        Text("Multi-satuan dan harga bertingkat hanya aktif di APK Grosir.")
                        products.filter { it.isActive }.forEach { product ->
                            ItemCard(
                                product.name,
                                "Satuan dasar: ${product.unitLabel}",
                                "Atur",
                            ) {
                                selectedProductId = product.id
                                dialog = "wholesale"
                            }
                        }
                    }
                    "Kuliner" -> {
                        ActionRow("Atur topping/resep" to { dialog = "culinary" })
                        SectionTitle("Antrean pesanan")
                        orders.forEach { sale ->
                            val next = when (sale.orderStatus) {
                                "NEW" -> OrderStatus.PROCESSING
                                "PROCESSING" -> OrderStatus.READY
                                "READY" -> OrderStatus.COMPLETED
                                else -> null
                            }
                            ItemCard(
                                sale.receiptNumber,
                                "${orderLabel(sale.orderStatus)} • ${formatRupiah(sale.total)}",
                                next?.let(::orderLabel),
                            ) {
                                next?.let { viewModel.moveOrder(sale.id, it) }
                            }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "category" -> TextInputDialog(
            title = if (selectedCategoryId == null) "Tambah kategori" else "Ubah kategori",
            labels = listOf("Nama kategori"),
            initialValues = listOf(
                categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty(),
            ),
            onDismiss = { dialog = null },
        ) {
            viewModel.saveCategory(selectedCategoryId, it[0])
            dialog = null
        }
        "product" -> ProductDialog(
            categories = categories.map { it.id to it.name },
            product = products.firstOrNull { it.id == selectedProductId },
            onDismiss = { dialog = null },
            onSave = { categoryId, name, price, stock, unit, imageUri ->
                viewModel.saveProduct(
                    selectedProductId,
                    categoryId,
                    name,
                    price,
                    stock,
                    unit,
                    imageUri,
                )
                dialog = null
            },
        )
        "variant" -> VariantDialog(
            products = products.filter { it.isActive }.map { it.id to it.name },
            onDismiss = { dialog = null },
        ) { productId, label, price, stock ->
            viewModel.saveVariant(productId, label, price, stock)
            dialog = null
        }
        "stock" -> StockDialog(
            variants = variants
                .filter { it.isActive && it.productId == selectedProductId }
                .map { it.id to it.label },
            onDismiss = { dialog = null },
            onSave = { variantId, delta, type, reason ->
                viewModel.adjustStock(
                    requireNotNull(selectedProductId),
                    variantId,
                    delta,
                    type,
                    reason,
                )
                dialog = null
            },
        )
        "supplier" -> PartyDialog(
            title = "Tambah pemasok",
            onDismiss = { dialog = null },
        ) { name, phone, address ->
            viewModel.saveParty(PartyKind.SUPPLIER, name, phone, address)
            dialog = null
        }
        "purchase" -> PurchaseDialog(
            suppliers = suppliers.map { it.id to it.name },
            products = products.filter { it.isActive }.map { it.id to it.name },
            variants = variants.filter { it.isActive }
                .map { Triple(it.id, it.productId, it.label) },
            onDismiss = { dialog = null },
        ) { supplierId, productId, variantId, quantity, cost, paid, note ->
            viewModel.recordPurchase(
                supplierId,
                productId,
                variantId,
                quantity,
                cost,
                paid,
                note,
            )
            dialog = null
        }
        "worker" -> WorkerDialog(
            onDismiss = { dialog = null },
        ) { name, phone, scheme, rate ->
            viewModel.saveEmployee(name, phone, scheme, rate)
            dialog = null
        }
        "attendance" -> AttendanceDialog(
            onDismiss = { dialog = null },
        ) { status, overtime, bonus, deduction, advance ->
            viewModel.recordAttendance(
                requireNotNull(selectedEmployee),
                status,
                overtime,
                bonus,
                deduction,
                advance,
            )
            dialog = null
        }
        "rate" -> TextInputDialog(
            title = "Ubah tarif harian",
            labels = listOf("Tarif baru"),
            numericIndexes = setOf(0),
            onDismiss = { dialog = null },
        ) {
            viewModel.updateDailyRate(
                requireNotNull(selectedEmployee).id,
                it[0].toLongOrNull() ?: 0,
            )
            dialog = null
        }
        "job" -> TextInputDialog(
            title = "Tambah pekerjaan panggilan",
            labels = listOf("Nama pekerjaan", "Nilai kesepakatan"),
            numericIndexes = setOf(1),
            onDismiss = { dialog = null },
        ) {
            viewModel.createFreelanceJob(
                requireNotNull(selectedEmployee),
                it[0],
                it[1].toLongOrNull() ?: 0,
            )
            dialog = null
        }
        "pay_job" -> TextInputDialog(
            title = "Bayar pekerjaan panggilan",
            labels = listOf("Nominal cicilan"),
            numericIndexes = setOf(0),
            onDismiss = { dialog = null },
        ) {
            viewModel.payFreelanceJob(
                requireNotNull(selectedJobId),
                it[0].toLongOrNull() ?: 0,
            )
            dialog = null
        }
        "wholesale" -> WholesaleDialog(
            onDismiss = { dialog = null },
        ) { mode, label, factorOrMinimum, price ->
            val productId = requireNotNull(selectedProductId)
            if (mode == 0) {
                viewModel.saveUnit(productId, label, factorOrMinimum, price)
            } else {
                viewModel.savePriceTier(productId, factorOrMinimum, price)
            }
            dialog = null
        }
        "culinary" -> CulinarySetupDialog(
            products = products.filter { it.isActive }.map { it.id to it.name },
            onDismiss = { dialog = null },
            onTopping = { productId, label, price ->
                viewModel.saveTopping(productId, label, price)
                dialog = null
            },
            onRecipe = { menuId, ingredientId, quantity ->
                viewModel.saveRecipe(menuId, ingredientId, quantity)
                dialog = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: OperationsViewModel,
    startTab: Int = 0,
    title: String = "Keuangan",
) {
    val cash by viewModel.cashEntries.collectAsState()
    val shifts by viewModel.shifts.collectAsState()
    val shiftSummary by viewModel.shiftSummary.collectAsState()
    val shiftLoading by viewModel.shiftLoading.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val saleDetail by viewModel.saleDetail.collectAsState()
    val tabs = listOf("Kas", "Utang & Piutang", "Transaksi")
    var tab by remember(startTab) {
        mutableIntStateOf(startTab.coerceIn(tabs.indices))
    }
    var dialog by remember { mutableStateOf<String?>(null) }
    var debtToPay by remember { mutableStateOf<DebtEntity?>(null) }
    var transactionSearch by remember { mutableStateOf("") }

    LaunchedEffect(shifts, cash, sales) {
        viewModel.refreshShiftSummary()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                colors = ownerTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OwnerSectionTabs(
                items = tabs,
                selectedIndex = tab,
                onSelected = { tab = it },
                testTag = "finance-section-grid",
            )
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    0 -> {
                        ShiftSection(
                            shifts = shifts,
                            summary = shiftSummary,
                            isLoading = shiftLoading,
                            onOpenRequest = { dialog = "open_shift" },
                            onCloseRequest = { dialog = "close_shift" },
                        )
                        SectionTitle("Catatan kas")
                        Button(
                            onClick = { dialog = "cash" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        ) { Text("Tambah catatan kas") }
                        if (cash.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada catatan kas",
                                message = "Catat uang masuk atau keluar yang bukan berasal dari transaksi kasir.",
                                actionLabel = "Tambah catatan kas",
                                onAction = { dialog = "cash" },
                                testTag = "cash-empty-state",
                            )
                        } else cash.forEach {
                            OwnerDetailCard(
                                "${cashLabel(it.type)} • ${formatRupiah(it.amount)}",
                                "${it.category} • ${formatDate(it.createdAt)}",
                            )
                        }
                    }
                    1 -> {
                        val payableTotal = debts.filter { it.kind == DebtKind.PAYABLE.name }
                            .sumOf { (it.originalAmount - it.paidAmount).coerceAtLeast(0) }
                        val receivableTotal = debts.filter { it.kind == DebtKind.RECEIVABLE.name }
                            .sumOf { (it.originalAmount - it.paidAmount).coerceAtLeast(0) }
                        OwnerHeroCard(
                            eyebrow = if (viewModel.capabilities.customerReceivables) {
                                "Piutang belum diterima"
                            } else {
                                "Utang belum dibayar"
                            },
                            value = formatRupiah(
                                if (viewModel.capabilities.customerReceivables) receivableTotal else payableTotal,
                            ),
                            supportingText = "Tampilkan tagihan yang masih perlu diselesaikan.",
                        ) {
                            OwnerMetricStrip(
                                buildList {
                                    add(formatRupiah(payableTotal) to "Utang")
                                    if (viewModel.capabilities.customerReceivables) {
                                        add(formatRupiah(receivableTotal) to "Piutang")
                                    }
                                    add(debts.count { it.originalAmount > it.paidAmount }.toString() to "Belum lunas")
                                },
                            )
                        }
                        if (viewModel.capabilities.customerReceivables) {
                            Button(
                                onClick = { dialog = "receivable" },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) { Text("Tambah piutang") }
                            OutlinedButton(
                                onClick = { dialog = "customer" },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            ) { Text("Tambah pelanggan") }
                        }
                        OutlinedButton(
                            onClick = { dialog = "payable" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) { Text("Tambah utang") }
                        SectionTitle("Daftar utang & piutang")
                        if (debts.isEmpty()) {
                            OwnerEmptyState(
                                title = "Belum ada utang atau piutang",
                                message = "Tagihan yang dicatat akan tampil di sini beserta sisa pembayarannya.",
                                actionLabel = if (viewModel.capabilities.customerReceivables) "Tambah piutang" else "Tambah utang",
                                onAction = {
                                    dialog = if (viewModel.capabilities.customerReceivables) "receivable" else "payable"
                                },
                                testTag = "debt-empty-state",
                            )
                        } else debts.forEach { debt ->
                            val remaining = (debt.originalAmount - debt.paidAmount).coerceAtLeast(0)
                            OwnerDetailCard(
                                "${if (debt.kind == DebtKind.PAYABLE.name) "Utang" else "Piutang"} • ${debt.partyName}",
                                "Sisa ${formatRupiah(remaining)} • ${settlementLabel(debt.settlementStatus)}",
                                if (remaining > 0) "Catat bayar" else null,
                            ) {
                                debtToPay = debt
                                dialog = "pay"
                            }
                        }
                    }
                    else -> {
                        val visibleSales = sales.filter {
                            transactionSearch.isBlank() || it.receiptNumber.contains(transactionSearch, ignoreCase = true)
                        }
                        OwnerHeroCard(
                            eyebrow = "Riwayat transaksi",
                            value = "${sales.size} transaksi",
                            supportingText = "Total penjualan tercatat ${formatRupiah(sales.sumOf { it.total })}.",
                        )
                        OutlinedTextField(
                            value = transactionSearch,
                            onValueChange = { transactionSearch = it },
                            label = { Text("Cari nomor struk") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("transaction-search"),
                        )
                        if (visibleSales.isEmpty()) {
                            OwnerEmptyState(
                                title = if (sales.isEmpty()) "Belum ada transaksi" else "Transaksi tidak ditemukan",
                                message = if (sales.isEmpty()) {
                                    "Transaksi yang selesai dari kasir akan muncul otomatis di sini."
                                } else {
                                    "Coba periksa kembali nomor struk yang dicari."
                                },
                                testTag = "transaction-empty-state",
                            )
                        } else visibleSales.forEach {
                            OwnerDetailCard(
                                it.receiptNumber,
                                "${formatRupiah(it.total)} • ${it.paymentMethod} • ${formatDate(it.createdAt)}",
                                "Lihat detail",
                            ) { viewModel.openSaleDetail(it) }
                        }
                    }
                }
            }
        }
    }

    when (dialog) {
        "open_shift" -> ShiftOpenDialog(
            onDismiss = { dialog = null },
        ) { cashierName, openingCash, note ->
            viewModel.openShift(cashierName, openingCash, note)
            dialog = null
        }
        "close_shift" -> ShiftCloseDialog(
            onDismiss = { dialog = null },
        ) { closingCash, note ->
            viewModel.closeShift(closingCash, note)
            dialog = null
        }
        "cash" -> CashDialog(
            onDismiss = { dialog = null },
        ) { type, amount, category, note ->
            viewModel.addCash(type, amount, category, note)
            dialog = null
        }
        "customer" -> PartyDialog(
            title = "Tambah pelanggan",
            onDismiss = { dialog = null },
        ) { name, phone, address ->
            viewModel.saveParty(PartyKind.CUSTOMER, name, phone, address)
            dialog = null
        }
        "payable", "receivable" -> DebtDialog(
            title = if (dialog == "payable") "Tambah utang" else "Tambah piutang",
            parties = if (dialog == "payable") {
                suppliers.map { it.id to it.name }
            } else {
                customers.map { it.id to it.name }
            },
            onDismiss = { dialog = null },
        ) { partyId, amount, initial, note ->
            viewModel.createDebt(
                if (dialog == "payable") DebtKind.PAYABLE else DebtKind.RECEIVABLE,
                partyId,
                amount,
                initial,
                note,
            )
            dialog = null
        }
        "pay" -> TextInputDialog(
            title = "Bayar tagihan",
            labels = listOf("Nominal pembayaran"),
            numericIndexes = setOf(0),
            onDismiss = { dialog = null },
        ) {
            viewModel.payDebt(requireNotNull(debtToPay), it[0].toLongOrNull() ?: 0)
            dialog = null
        }
    }
    saleDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = viewModel::closeSaleDetail,
            title = { Text(detail.sale.receiptNumber) },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Waktu: ${formatDate(detail.sale.createdAt)}")
                    Text("Metode: ${detail.sale.paymentMethod}")
                    detail.items.forEach { item ->
                        InfoCard(
                            item.productName,
                            "${item.quantity} ${item.unitLabel} × ${formatRupiah(item.unitPrice)} = ${formatRupiah(item.subtotal)}",
                        )
                    }
                    HorizontalDivider()
                    Text(
                        "Total ${formatRupiah(detail.sale.total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (detail.sale.paymentMethod == "CREDIT") {
                        Text("Dibayar awal ${formatRupiah(detail.sale.amountReceived)}")
                        Text("Sisa ${formatRupiah(detail.sale.total - detail.sale.amountReceived)}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = viewModel::closeSaleDetail) { Text("Tutup") }
            },
        )
    }
}

private enum class ReportDetailMode {
    CASH,
    FULL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: OperationsViewModel) {
    val hasPin by viewModel.reportHasPin.collectAsState()
    val summary by viewModel.reportSummary.collectAsState()
    val previousSummary by viewModel.previousReportSummary.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()
    val reportChartMode by viewModel.reportChartMode.collectAsState()
    val reportChartGranularity by viewModel.reportChartGranularity.collectAsState()
    val reportProductMeasure by viewModel.reportProductMeasure.collectAsState()
    val selectedReportProductId by viewModel.selectedReportProductId.collectAsState()
    val reportTrend by viewModel.reportTrend.collectAsState()
    val reportTrendError by viewModel.reportTrendError.collectAsState()
    val forecastReport by viewModel.forecastReport.collectAsState()
    val forecastLoading by viewModel.forecastLoading.collectAsState()
    val forecastError by viewModel.forecastError.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val excelUri by viewModel.excelUri.collectAsState()
    val excelError by viewModel.excelError.collectAsState()
    val context = LocalContext.current
    val periods = remember { ReportPeriod.values().toList() }
    var pin by remember { mutableStateOf("") }
    var detailMode by remember { mutableStateOf<ReportDetailMode?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan", fontWeight = FontWeight.Bold) },
                colors = ownerTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (summary == null) {
                Text(
                    if (hasPin == false) "Buat PIN Owner 4-8 angka" else "Masukkan PIN Owner",
                    style = MaterialTheme.typography.titleMedium,
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.length <= 8 && value.all(Char::isDigit)) pin = value
                    },
                    label = { Text("PIN Owner") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        if (hasPin == false) viewModel.createReportPin(pin)
                        else viewModel.unlockReport(pin)
                        pin = ""
                    },
                    enabled = pin.length in 4..8,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (hasPin == false) "Buat PIN & Buka" else "Buka Laporan")
                }
            } else {
                ReportPeriodPicker(
                    periods = periods,
                    selected = reportPeriod,
                    onSelected = viewModel::selectReportPeriod,
                )
                OutlinedButton(
                    onClick = viewModel::refreshReport,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("report-refresh"),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Muat ulang laporan")
                }
                Text(
                    "Setelah ada transaksi baru, tekan tombol ini untuk mengambil data laporan terbaru.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    onClick = viewModel::createExcelExport,
                    enabled = !busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("excel-export"),
                ) {
                    Icon(Icons.Outlined.TableView, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(if (busy) "Menyiapkan Excel..." else "Simpan Laporan Excel")
                }
                Text(
                    "Simpan laporan ${reportPeriod.label.lowercase(Locale.forLanguageTag("id-ID"))} sebagai berkas Excel.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                excelError?.let { error ->
                    Text(
                        "Gagal menyimpan Excel: $error",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (excelUri != null) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND).apply {
                                        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                        putExtra(Intent.EXTRA_STREAM, excelUri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    },
                                    "Bagikan Excel",
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Bagikan Excel")
                    }
                }
                ReportOverviewCard(
                    summary = requireNotNull(summary),
                    previous = previousSummary,
                    period = reportPeriod,
                )
                ReportSalesMovementCard(
                    trend = reportTrend,
                    period = reportPeriod,
                    error = reportTrendError,
                )
                OwnerLinkCard(
                    title = "Lihat arus kas",
                    subtitle = "Ringkasan uang masuk dan keluar",
                    onClick = {
                        detailMode = if (detailMode == ReportDetailMode.CASH) null else ReportDetailMode.CASH
                    },
                    testTag = "report-cash-details",
                )
                if (detailMode == ReportDetailMode.CASH) {
                    OwnerHeroCard(
                        eyebrow = "Selisih kas tercatat",
                        value = formatRupiah(requireNotNull(summary).netCash),
                        supportingText = "Uang masuk dikurangi uang keluar.",
                    ) {
                        OwnerMetricStrip(
                            listOf(
                                formatRupiah(requireNotNull(summary).cashIn) to "Uang masuk",
                                formatRupiah(requireNotNull(summary).cashOut) to "Uang keluar",
                                formatRupiah(requireNotNull(summary).expenses) to "Pengeluaran",
                            ),
                        )
                    }
                }
                OwnerLinkCard(
                    title = "Lihat rincian lengkap",
                    subtitle = "Pembayaran, produk, perkiraan, dan saldo",
                    onClick = {
                        detailMode = if (detailMode == ReportDetailMode.FULL) null else ReportDetailMode.FULL
                    },
                    testTag = "report-full-details",
                )
                if (detailMode == ReportDetailMode.FULL) {
                    SectionTitle("Rincian lengkap")
                    OwnerMetricStrip(
                        buildList {
                            add(formatRupiah(requireNotNull(summary).outstandingPayables) to "Utang")
                            if (viewModel.capabilities.customerReceivables) {
                                add(formatRupiah(requireNotNull(summary).outstandingReceivables) to "Piutang")
                            }
                        },
                    )
                    ReportTrendSection(
                        trend = reportTrend,
                        mode = reportChartMode,
                        granularity = reportChartGranularity,
                        productMeasure = reportProductMeasure,
                        selectedProductId = selectedReportProductId,
                        error = reportTrendError,
                        onModeSelected = viewModel::selectReportChartMode,
                        onGranularitySelected = viewModel::selectReportChartGranularity,
                        onProductMeasureSelected = viewModel::selectReportProductMeasure,
                        onProductSelected = viewModel::selectReportProduct,
                    )
                    SectionTitle("Cara pembayaran pelanggan")
                    val reportPayments = requireNotNull(summary).payments
                    PaymentMethodBarChart(reportPayments)
                    reportPayments.forEach {
                        OwnerDetailCard(it.paymentMethod, formatRupiah(it.total))
                    }
                    SalesForecastSection(
                        report = forecastReport,
                        isLoading = forecastLoading,
                        error = forecastError,
                    )
                    OwnerEmptyState(
                        title = "Laba belum dihitung",
                        message = "Metode HPP belum ditentukan, jadi angka omzet tidak boleh dianggap sebagai laba.",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: OperationsViewModel,
    onExitOwner: () -> Unit,
) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val backupUri by viewModel.backupUri.collectAsState()
    val preview by viewModel.backupPreview.collectAsState()
    var showProfile by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        viewModel.finishRestoreFileSelection(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lainnya", fontWeight = FontWeight.Bold) },
                colors = ownerTopAppBarColors(),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Profil usaha")
            OwnerHeroCard(
                eyebrow = "Nama usaha",
                value = profile?.businessName ?: "Memuat…",
                supportingText = "Jenis usaha: ${businessTypeLabel(profile?.businessType.orEmpty())}",
            )
            OutlinedButton(
                onClick = { showProfile = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text("Ubah nama usaha")
            }
            OwnerSectionDivider()
            SectionTitle("Salinan & keamanan data")
            OwnerHeroCard(
                eyebrow = "Status salinan data",
                value = if (backupUri == null) "Belum ada salinan baru" else "Salinan siap dibagikan",
                supportingText = if (backupUri == null) {
                    "Buat salinan data agar catatan usaha tidak hilang saat HP rusak atau hilang."
                } else {
                    "Bagikan salinan ke tempat lain, jangan hanya disimpan di HP ini."
                },
            )
            Button(
                onClick = viewModel::createBackup,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(if (busy) "Menyiapkan salinan..." else "Buat salinan sekarang")
            }
            if (backupUri != null) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/zip"
                                    putExtra(Intent.EXTRA_STREAM, backupUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "Bagikan salinan keluar HP",
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Bagikan salinan data")
                }
            }
            OwnerEmptyState(
                title = "Pulihkan data dari salinan",
                message = "Data sekarang akan diamankan dulu. Isi salinan akan diperiksa sebelum mengganti data aktif.",
                actionLabel = "Pilih berkas salinan",
                onAction = {
                    viewModel.beginRestoreFileSelection()
                    try {
                        openBackup.launch(arrayOf("*/*"))
                    } catch (error: Exception) {
                        viewModel.finishRestoreFileSelection(null)
                        throw error
                    }
                },
                testTag = "restore-entry",
            )
            OwnerSectionDivider()
            SectionTitle("Keamanan Owner")
            OutlinedButton(
                onClick = { showChangePin = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("report-change-pin"),
            ) { Text("Ganti PIN Owner") }
            OutlinedButton(
                onClick = onExitOwner,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("owner-exit"),
            ) {
                Text("Keluar Mode Owner")
            }
            Text(
                "Aplikasi tetap bisa dipakai tanpa akun dan tanpa internet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (showProfile) {
        TextInputDialog(
            title = "Ubah nama usaha",
            labels = listOf("Nama usaha"),
            initialValues = listOf(profile?.businessName.orEmpty()),
            onDismiss = { showProfile = false },
        ) {
            viewModel.saveProfile(it[0])
            showProfile = false
        }
    }
    if (showChangePin) {
        TextInputDialog(
            title = "Ganti PIN Owner",
            labels = listOf("PIN lama", "PIN baru 4-8 angka"),
            numericIndexes = setOf(0, 1),
            onDismiss = { showChangePin = false },
        ) {
            viewModel.changeReportPin(it[0], it[1])
            showChangePin = false
        }
    }
    preview?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Konfirmasi pemulihan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Usaha: ${item.manifest.businessName}")
                    Text("Jenis: ${item.manifest.businessType}")
                    Text("Dibuat: ${formatDate(item.manifest.createdAt)}")
                    Text("Data aktif akan diamankan dulu sebelum diganti.")
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRestore) { Text("Pulihkan sekarang") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun ActionRow(vararg actions: Pair<String, () -> Unit>) {
    CompactGrid(actions.toList())
}

@Composable
private fun CompactGrid(
    items: List<Pair<String, () -> Unit>>,
    selectedIndex: Int? = null,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.chunked(2).forEachIndexed { rowIndex, rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEachIndexed { columnIndex, (label, action) ->
                    val itemIndex = rowIndex * 2 + columnIndex
                    val buttonModifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                    if (selectedIndex == itemIndex) {
                        Button(
                            onClick = action,
                            modifier = buttonModifier,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = action,
                            modifier = buttonModifier,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Text(
                                label,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<com.bimacore.usahakecil.data.CategoryEntity>,
    onEdit: (com.bimacore.usahakecil.data.CategoryEntity) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("category-grid"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.chunked(2).forEach { rowItems ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { category ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 92.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                category.name,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Aktif",
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                TextButton(
                                    onClick = { onEdit(category) },
                                    contentPadding = PaddingValues(0.dp),
                                ) { Text("Ubah") }
                            }
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
fun InfoCard(title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ItemCard(
    title: String,
    subtitle: String,
    action: String?,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
    onAction: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (action != null) TextButton(onClick = onAction) { Text(action) }
            if (secondaryAction != null) {
                TextButton(onClick = onSecondaryAction) { Text(secondaryAction) }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    labels: List<String>,
    numericIndexes: Set<Int> = emptySet(),
    initialValues: List<String> = List(labels.size) { "" },
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var values by remember { mutableStateOf(initialValues) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                labels.forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = values.getOrElse(index) { "" },
                        onValueChange = { value ->
                            if (index !in numericIndexes || value.all(Char::isDigit)) {
                                values = values.toMutableList().also { it[index] = value }
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (index in numericIndexes) {
                                KeyboardType.Number
                            } else {
                                KeyboardType.Text
                            },
                        ),
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(values) }) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun ProductDialog(
    categories: List<Pair<Long, String>>,
    product: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (Long, String, Long, Int, String, String?) -> Unit,
) {
    var selectedImageUri by remember(product?.id, product?.imageUri) {
        mutableStateOf(product?.imageUri)
    }
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            selectedImageUri = uri.toString()
        }
    }
    var categoryIndex by remember {
        mutableIntStateOf(
            categories.indexOfFirst { it.first == product?.categoryId }.coerceAtLeast(0),
        )
    }
    var values by remember {
        mutableStateOf(
            listOf(
                product?.name.orEmpty(),
                product?.basePrice?.toString().orEmpty(),
                product?.stock?.toString().orEmpty(),
                product?.unitLabel ?: "pcs",
            ),
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product == null) "Tambah produk" else "Ubah produk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (categories.isNotEmpty()) categoryIndex = (categoryIndex + 1) % categories.size
                    },
                ) {
                    Text("Kategori: ${categories.getOrNull(categoryIndex)?.second ?: "buat kategori dulu"}")
                }
                OutlinedButton(
                    onClick = { imagePicker.launch(arrayOf("image/*")) },
                    modifier = Modifier.testTag("product-image-picker"),
                ) {
                    Text(if (selectedImageUri == null) "Pilih foto menu" else "Ganti foto menu")
                }
                selectedImageUri?.let { uri ->
                    ProductVisual(
                        imageUri = uri,
                        icon = Icons.Outlined.Inventory2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                    )
                }
                listOf("Nama produk", "Harga jual", "Stok awal", "Satuan").forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = values[index],
                        onValueChange = { value ->
                            if (index !in 1..2 || value.all(Char::isDigit)) {
                                values = values.toMutableList().also { it[index] = value }
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (index in 1..2) KeyboardType.Number else KeyboardType.Text,
                        ),
                        enabled = product == null || index != 2,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = categories.isNotEmpty(),
                onClick = {
                    onSave(
                        categories[categoryIndex].first,
                        values[0],
                        values[1].toLongOrNull() ?: 0,
                        values[2].toIntOrNull() ?: 0,
                        values[3],
                        selectedImageUri,
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun VariantDialog(
    products: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long, String, Long?, Int) -> Unit,
) {
    var productIndex by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(listOf("", "", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah varian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton("Produk", products, productIndex) {
                    productIndex = nextIndex(productIndex, products.size)
                }
                listOf("Nama varian", "Harga khusus (boleh kosong)", "Stok awal")
                    .forEachIndexed { index, label ->
                        OutlinedTextField(
                            value = values[index],
                            onValueChange = { value ->
                                if (index == 0 || value.all(Char::isDigit)) {
                                    values = values.toMutableList().also { it[index] = value }
                                }
                            },
                            label = { Text(label) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (index == 0) {
                                    KeyboardType.Text
                                } else {
                                    KeyboardType.Number
                                },
                            ),
                        )
                    }
            }
        },
        confirmButton = {
            Button(
                enabled = products.isNotEmpty(),
                onClick = {
                    onSave(
                        products[productIndex].first,
                        values[0],
                        values[1].toLongOrNull(),
                        values[2].toIntOrNull() ?: 0,
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun StockDialog(
    variants: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long?, Int, String, String) -> Unit,
) {
    val types = listOf("ADJUSTMENT_IN", "ADJUSTMENT_OUT", "DAMAGED", "LOST")
    var variantIndex by remember { mutableIntStateOf(0) }
    var typeIndex by remember { mutableIntStateOf(0) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Penyesuaian stok") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (variants.isNotEmpty()) {
                    SelectionButton("Varian", variants, variantIndex) {
                        variantIndex = nextIndex(variantIndex, variants.size)
                    }
                }
                OutlinedButton(onClick = { typeIndex = (typeIndex + 1) % types.size }) {
                    Text("Jenis: ${stockTypeLabel(types[typeIndex])}")
                }
                OutlinedTextField(
                    quantity,
                    { if (it.all(Char::isDigit)) quantity = it },
                    label = { Text("Jumlah") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(reason, { reason = it }, label = { Text("Alasan wajib") })
            }
        },
        confirmButton = {
            Button(onClick = {
                val raw = quantity.toIntOrNull() ?: 0
                val delta = if (types[typeIndex] == "ADJUSTMENT_IN") raw else -raw
                onSave(
                    variants.getOrNull(variantIndex)?.first,
                    delta,
                    types[typeIndex],
                    reason,
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun PartyDialog(
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
) = TextInputDialog(
    title,
    listOf("Nama", "Nomor HP", "Alamat"),
    onDismiss = onDismiss,
) { onSave(it[0], it[1], it[2]) }

@Composable
private fun PurchaseDialog(
    suppliers: List<Pair<Long, String>>,
    products: List<Pair<Long, String>>,
    variants: List<Triple<Long, Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long?, Int, Long, Long, String) -> Unit,
) {
    var supplierIndex by remember { mutableIntStateOf(0) }
    var productIndex by remember { mutableIntStateOf(0) }
    var variantIndex by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(listOf("", "", "", "")) }
    val productVariants = variants
        .filter { it.second == products.getOrNull(productIndex)?.first }
        .map { it.first to it.third }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat pembelian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton("Pemasok", suppliers, supplierIndex) {
                    supplierIndex = nextIndex(supplierIndex, suppliers.size)
                }
                SelectionButton("Produk", products, productIndex) {
                    productIndex = nextIndex(productIndex, products.size)
                    variantIndex = 0
                }
                if (productVariants.isNotEmpty()) {
                    SelectionButton("Varian", productVariants, variantIndex) {
                        variantIndex = nextIndex(variantIndex, productVariants.size)
                    }
                }
                listOf("Jumlah", "Harga beli/satuan", "Sudah dibayar", "Catatan").forEachIndexed { index, label ->
                    OutlinedTextField(
                        values[index],
                        {
                            if (index == 3 || it.all(Char::isDigit)) {
                                values = values.toMutableList().also { list -> list[index] = it }
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (index < 3) KeyboardType.Number else KeyboardType.Text,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = suppliers.isNotEmpty() && products.isNotEmpty(),
                onClick = {
                    onSave(
                        suppliers[supplierIndex].first,
                        products[productIndex].first,
                        productVariants.getOrNull(variantIndex)?.first,
                        values[0].toIntOrNull() ?: 0,
                        values[1].toLongOrNull() ?: 0,
                        values[2].toLongOrNull() ?: 0,
                        values[3],
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun WorkerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, WorkerScheme, Long?) -> Unit,
) {
    var scheme by remember { mutableStateOf(WorkerScheme.DAILY) }
    var values by remember { mutableStateOf(listOf("", "", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah pekerja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scheme = if (scheme == WorkerScheme.DAILY) {
                        WorkerScheme.FREELANCE
                    } else {
                        WorkerScheme.DAILY
                    }
                }) { Text(if (scheme == WorkerScheme.DAILY) "Pekerja harian" else "Pekerja panggilan") }
                listOf("Nama", "Nomor HP", "Tarif harian").forEachIndexed { index, label ->
                    if (index < 2 || scheme == WorkerScheme.DAILY) {
                        OutlinedTextField(
                            values[index],
                            {
                                if (index != 2 || it.all(Char::isDigit)) {
                                    values = values.toMutableList().also { list -> list[index] = it }
                                }
                            },
                            label = { Text(label) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (index == 2) KeyboardType.Number else KeyboardType.Text,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(values[0], values[1], scheme, values[2].toLongOrNull())
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun AttendanceDialog(
    onDismiss: () -> Unit,
    onSave: (AttendanceStatus, Long, Long, Long, Long) -> Unit,
) {
    val statuses = AttendanceStatus.entries
    var statusIndex by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(List(4) { "" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat kehadiran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { statusIndex = (statusIndex + 1) % statuses.size }) {
                    Text("Status: ${attendanceLabel(statuses[statusIndex])}")
                }
                listOf("Lembur", "Bonus", "Potongan", "Kasbon").forEachIndexed { index, label ->
                    OutlinedTextField(
                        values[index],
                        {
                            if (it.all(Char::isDigit)) {
                                values = values.toMutableList().also { list -> list[index] = it }
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    statuses[statusIndex],
                    values[0].toLongOrNull() ?: 0,
                    values[1].toLongOrNull() ?: 0,
                    values[2].toLongOrNull() ?: 0,
                    values[3].toLongOrNull() ?: 0,
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun WholesaleDialog(
    onDismiss: () -> Unit,
    onSave: (Int, String, Int, Long) -> Unit,
) {
    var mode by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(listOf("", "", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == 0) "Tambah satuan" else "Tambah harga bertingkat") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { mode = 1 - mode }) {
                    Text(if (mode == 0) "Mode: satuan pak/dus" else "Mode: harga minimum jumlah")
                }
                if (mode == 0) {
                    OutlinedTextField(values[0], {
                        values = values.toMutableList().also { list -> list[0] = it }
                    }, label = { Text("Nama satuan") })
                }
                OutlinedTextField(
                    values[1],
                    {
                        if (it.all(Char::isDigit)) {
                            values = values.toMutableList().also { list -> list[1] = it }
                        }
                    },
                    label = { Text(if (mode == 0) "Isi pcs per satuan" else "Minimum pcs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    values[2],
                    {
                        if (it.all(Char::isDigit)) {
                            values = values.toMutableList().also { list -> list[2] = it }
                        }
                    },
                    label = { Text(if (mode == 0) "Harga per satuan" else "Harga per pcs") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    mode,
                    values[0].ifBlank { "Harga tier" },
                    values[1].toIntOrNull() ?: 0,
                    values[2].toLongOrNull() ?: 0,
                )
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun CulinarySetupDialog(
    products: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onTopping: (Long, String, Long) -> Unit,
    onRecipe: (Long, Long, Int) -> Unit,
) {
    var mode by remember { mutableIntStateOf(0) }
    var firstIndex by remember { mutableIntStateOf(0) }
    var secondIndex by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == 0) "Tambah topping" else "Tambah bahan resep") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { mode = 1 - mode }) {
                    Text(if (mode == 0) "Mode: topping" else "Mode: resep bahan")
                }
                SelectionButton("Menu", products, firstIndex) {
                    firstIndex = nextIndex(firstIndex, products.size)
                }
                if (mode == 0) {
                    OutlinedTextField(label, { label = it }, label = { Text("Nama topping") })
                } else {
                    SelectionButton("Bahan", products, secondIndex) {
                        secondIndex = nextIndex(secondIndex, products.size)
                    }
                }
                OutlinedTextField(
                    amount,
                    { if (it.all(Char::isDigit)) amount = it },
                    label = { Text(if (mode == 0) "Harga topping" else "Jumlah bahan per menu") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = products.isNotEmpty(),
                onClick = {
                    if (mode == 0) {
                        onTopping(
                            products[firstIndex].first,
                            label,
                            amount.toLongOrNull() ?: 0,
                        )
                    } else {
                        onRecipe(
                            products[firstIndex].first,
                            products[secondIndex].first,
                            amount.toIntOrNull() ?: 0,
                        )
                    }
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun CashDialog(
    onDismiss: () -> Unit,
    onSave: (ManualCashType, Long, String, String) -> Unit,
) {
    val types = ManualCashType.entries
    var typeIndex by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(listOf("", "", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah catatan kas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { typeIndex = (typeIndex + 1) % types.size }) {
                    Text("Jenis: ${cashLabel(types[typeIndex].name)}")
                }
                listOf("Nominal", "Kategori", "Catatan").forEachIndexed { index, label ->
                    OutlinedTextField(
                        values[index],
                        {
                            if (index != 0 || it.all(Char::isDigit)) {
                                values = values.toMutableList().also { list -> list[index] = it }
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (index == 0) KeyboardType.Number else KeyboardType.Text,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(types[typeIndex], values[0].toLongOrNull() ?: 0, values[1], values[2])
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun DebtDialog(
    title: String,
    parties: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (Long, Long, Long, String) -> Unit,
) {
    var partyIndex by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(listOf("", "", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton("Pihak", parties, partyIndex) {
                    partyIndex = nextIndex(partyIndex, parties.size)
                }
                listOf("Nilai awal", "Pembayaran awal", "Catatan").forEachIndexed { index, label ->
                    OutlinedTextField(
                        values[index],
                        {
                            if (index == 2 || it.all(Char::isDigit)) {
                                values = values.toMutableList().also { list -> list[index] = it }
                            }
                        },
                        label = { Text(label) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (index < 2) KeyboardType.Number else KeyboardType.Text,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = parties.isNotEmpty(),
                onClick = {
                    onSave(
                        parties[partyIndex].first,
                        values[0].toLongOrNull() ?: 0,
                        values[1].toLongOrNull() ?: 0,
                        values[2],
                    )
                },
            ) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun SelectionButton(
    label: String,
    options: List<Pair<Long, String>>,
    index: Int,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick) {
        Text("$label: ${options.getOrNull(index)?.second ?: "belum ada data"}")
    }
}

private fun nextIndex(current: Int, size: Int): Int =
    if (size <= 0) 0 else (current + 1) % size

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")).format(Date(timestamp))

private fun businessTypeLabel(value: String): String = when (value.uppercase(Locale.ROOT)) {
    "RETAIL" -> "Toko & UMKM"
    "WHOLESALE" -> "Grosir & Agen"
    "CULINARY" -> "Kuliner & PKL"
    else -> value
}

private fun settlementLabel(value: String): String = when (value) {
    "PAID" -> "Lunas"
    "PARTIAL" -> "Sebagian"
    else -> "Belum dibayar"
}

private fun stockTypeLabel(value: String): String = when (value) {
    "ADJUSTMENT_IN" -> "Stok masuk"
    "ADJUSTMENT_OUT" -> "Stok keluar"
    "DAMAGED" -> "Rusak"
    "LOST" -> "Hilang"
    else -> value
}

private fun cashLabel(value: String): String = when (value) {
    "CASH_IN", "SALE_IN", "RECEIVABLE_IN" -> "Kas masuk"
    "EXPENSE" -> "Pengeluaran"
    else -> "Kas keluar"
}

private fun attendanceLabel(value: AttendanceStatus): String = when (value) {
    AttendanceStatus.PRESENT -> "Hadir"
    AttendanceStatus.HALF_DAY -> "Setengah hari"
    AttendanceStatus.LEAVE -> "Izin"
    AttendanceStatus.ABSENT -> "Tidak hadir"
}

private fun orderLabel(value: String): String = when (value) {
    "NEW" -> "Baru"
    "PROCESSING" -> "Diproses"
    "READY" -> "Siap"
    "COMPLETED" -> "Selesai"
    else -> value
}

private fun orderLabel(value: OrderStatus): String = orderLabel(value.name)
