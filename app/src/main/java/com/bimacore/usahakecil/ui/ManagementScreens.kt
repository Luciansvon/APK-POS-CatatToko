package com.bimacore.usahakecil.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bimacore.usahakecil.data.DebtEntity
import com.bimacore.usahakecil.data.DebtKind
import com.bimacore.usahakecil.data.EmployeeEntity
import com.bimacore.usahakecil.data.ManualCashType
import com.bimacore.usahakecil.data.PartyKind
import com.bimacore.usahakecil.data.ProductEntity
import com.bimacore.usahakecil.data.WorkerScheme
import com.bimacore.usahakecil.domain.AttendanceStatus
import com.bimacore.usahakecil.domain.OrderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
        topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(label) },
                    )
                }
            }
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
                        categories.forEach { category ->
                            ItemCard(
                                category.name,
                                "Kategori aktif",
                                "Edit",
                            ) {
                                selectedCategoryId = category.id
                                dialog = "category"
                            }
                        }
                        products.forEach { product ->
                            ItemCard(
                                title = product.name,
                                subtitle = "${formatRupiah(product.basePrice)} • stok ${product.stock} ${product.unitLabel}",
                                action = if (product.isActive) "Nonaktifkan" else "Aktifkan",
                                onAction = {
                                    viewModel.setProductActive(product.id, !product.isActive)
                                },
                                secondaryAction = "Edit",
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
                        Text("Pilih produk untuk mencatat stok masuk, keluar, rusak, atau hilang.")
                        products.filter { it.isActive }.forEach { product ->
                            ItemCard(
                                product.name,
                                "Stok ${product.stock} ${product.unitLabel}",
                                "Sesuaikan",
                            ) {
                                selectedProductId = product.id
                                dialog = "stock"
                            }
                        }
                        SectionTitle("Riwayat terbaru")
                        movements.take(30).forEach {
                            InfoCard(
                                "${it.type}: ${it.baseQuantityDelta}",
                                "${it.reason} • ${formatDate(it.createdAt)}",
                            )
                        }
                    }
                    "Pembelian" -> {
                        ActionRow(
                            "Tambah supplier" to { dialog = "supplier" },
                            "Catat pembelian" to { dialog = "purchase" },
                        )
                        suppliers.forEach { InfoCard(it.name, it.phone.ifBlank { "Supplier" }) }
                        SectionTitle("Riwayat pembelian")
                        purchases.forEach {
                            InfoCard(
                                it.supplierName,
                                "${formatRupiah(it.total)} • ${settlementLabel(it.settlementStatus)}",
                            )
                        }
                    }
                    "Pekerja" -> {
                        ActionRow("Tambah pekerja" to { dialog = "worker" })
                        employees.forEach { employee ->
                            ItemCard(
                                employee.name,
                                if (employee.scheme == "DAILY") "Pekerja harian" else "Freelancer",
                                if (employee.scheme == "DAILY") "Catat hadir" else "Tambah kerja",
                                secondaryAction = if (employee.scheme == "DAILY") "Ubah tarif" else null,
                                onSecondaryAction = {
                                    selectedEmployee = employee
                                    dialog = "rate"
                                },
                            ) {
                                selectedEmployee = employee
                                dialog = if (employee.scheme == "DAILY") "attendance" else "job"
                            }
                        }
                        SectionTitle("Kehadiran belum dibayar")
                        attendance.filter { !it.isPaid }.forEach {
                            ItemCard(
                                "Upah ${formatRupiah(it.netPay)}",
                                "${it.status} • ${formatDate(it.workDate)}",
                                "Bayar",
                            ) { viewModel.payAttendance(it.id) }
                        }
                        SectionTitle("Pekerjaan freelancer")
                        jobs.forEach {
                            ItemCard(
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
            title = if (selectedCategoryId == null) "Tambah kategori" else "Edit kategori",
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
            onSave = { categoryId, name, price, stock, unit ->
                viewModel.saveProduct(selectedProductId, categoryId, name, price, stock, unit)
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
            onDismiss = { dialog = null },
            onSave = { delta, type, reason ->
                viewModel.adjustStock(requireNotNull(selectedProductId), delta, type, reason)
                dialog = null
            },
        )
        "supplier" -> PartyDialog(
            title = "Tambah supplier",
            onDismiss = { dialog = null },
        ) { name, phone, address ->
            viewModel.saveParty(PartyKind.SUPPLIER, name, phone, address)
            dialog = null
        }
        "purchase" -> PurchaseDialog(
            suppliers = suppliers.map { it.id to it.name },
            products = products.filter { it.isActive }.map { it.id to it.name },
            onDismiss = { dialog = null },
        ) { supplierId, productId, quantity, cost, paid, note ->
            viewModel.recordPurchase(supplierId, productId, quantity, cost, paid, note)
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
            title = "Tambah pekerjaan freelancer",
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
            title = "Bayar pekerjaan freelancer",
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(title, fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, label ->
                    Tab(tab == index, { tab = index }, text = { Text(label) })
                }
            }
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (tab) {
                    0 -> {
                        ActionRow("Tambah catatan kas" to { dialog = "cash" })
                        cash.forEach {
                            InfoCard(
                                "${cashLabel(it.type)} • ${formatRupiah(it.amount)}",
                                "${it.category} • ${formatDate(it.createdAt)}",
                            )
                        }
                    }
                    1 -> {
                        ActionRow(
                            "Tambah utang" to { dialog = "payable" },
                            *if (viewModel.capabilities.customerReceivables) {
                                arrayOf("Tambah piutang" to { dialog = "receivable" })
                            } else {
                                emptyArray()
                            },
                            *if (viewModel.capabilities.customerReceivables) {
                                arrayOf("Tambah pelanggan" to { dialog = "customer" })
                            } else {
                                emptyArray()
                            },
                        )
                        debts.forEach { debt ->
                            val remaining = debt.originalAmount - debt.paidAmount
                            ItemCard(
                                "${if (debt.kind == "PAYABLE") "Utang" else "Piutang"} • ${debt.partyName}",
                                "Sisa ${formatRupiah(remaining)} • ${settlementLabel(debt.settlementStatus)}",
                                if (remaining > 0) "Bayar" else null,
                            ) {
                                debtToPay = debt
                                dialog = "pay"
                            }
                        }
                    }
                    else -> sales.forEach {
                        ItemCard(
                            it.receiptNumber,
                            "${formatRupiah(it.total)} • ${it.paymentMethod} • ${formatDate(it.createdAt)}",
                            "Detail",
                        ) { viewModel.openSaleDetail(it) }
                    }
                }
            }
        }
    }

    when (dialog) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: OperationsViewModel) {
    val hasPin by viewModel.reportHasPin.collectAsState()
    val summary by viewModel.reportSummary.collectAsState()
    var pin by remember { mutableStateOf("") }
    var showChangePin by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Laporan", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (summary == null) {
                Text(
                    if (hasPin == false) "Buat PIN Owner 4–8 angka" else "Masukkan PIN Owner",
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
                ReportValue("Omzet hari ini", formatRupiah(requireNotNull(summary).totalSales))
                ReportValue("Jumlah transaksi", requireNotNull(summary).transactionCount.toString())
                ReportValue("Kas masuk tercatat", formatRupiah(requireNotNull(summary).cashIn))
                ReportValue("Kas keluar tercatat", formatRupiah(requireNotNull(summary).cashOut))
                ReportValue("Pengeluaran", formatRupiah(requireNotNull(summary).expenses))
                ReportValue("Selisih kas", formatRupiah(requireNotNull(summary).netCash))
                ReportValue("Sisa utang", formatRupiah(requireNotNull(summary).outstandingPayables))
                if (viewModel.capabilities.customerReceivables) {
                    ReportValue(
                        "Sisa piutang",
                        formatRupiah(requireNotNull(summary).outstandingReceivables),
                    )
                }
                SectionTitle("Penerimaan per metode")
                requireNotNull(summary).payments.forEach {
                    InfoCard(it.paymentMethod, formatRupiah(it.total))
                }
                Text(
                    "Laba belum dihitung karena metode HPP belum ditentukan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::refreshReport) { Text("Muat ulang") }
                    OutlinedButton(onClick = { showChangePin = true }) { Text("Ganti PIN") }
                    Button(onClick = viewModel::lockReport) { Text("Kunci Mode Owner") }
                }
            }
        }
    }
    if (showChangePin) {
        TextInputDialog(
            title = "Ganti PIN Owner",
            labels = listOf("PIN lama", "PIN baru 4–8 angka"),
            numericIndexes = setOf(0, 1),
            onDismiss = { showChangePin = false },
        ) {
            viewModel.changeReportPin(it[0], it[1])
            showChangePin = false
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
    val backupUri by viewModel.backupUri.collectAsState()
    val preview by viewModel.backupPreview.collectAsState()
    var showProfile by remember { mutableStateOf(false) }
    val openBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(viewModel::inspectBackup)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Lainnya", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle("Profil usaha")
            InfoCard(
                profile?.businessName ?: "Memuat…",
                "${profile?.businessType.orEmpty()} • ID ${profile?.businessUid?.take(8).orEmpty()}",
            )
            OutlinedButton(onClick = { showProfile = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Ubah nama usaha")
            }
            HorizontalDivider()
            SectionTitle("Backup & restore")
            Button(onClick = viewModel::createBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Buat backup lokal")
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
                                "Bagikan backup keluar HP",
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Bagikan backup") }
            }
            OutlinedButton(
                onClick = { openBackup.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Pilih file untuk restore") }
            Text(
                "Saran: bagikan backup keluar HP secara berkala. Backup di HP yang sama tidak melindungi saat HP hilang atau rusak.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            SectionTitle("Aplikasi")
            OutlinedButton(
                onClick = onExitOwner,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Keluar Mode Owner")
            }
            Text("Offline-first • tanpa akun • tanpa internet")
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
    preview?.let { item ->
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text("Konfirmasi restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Usaha: ${item.manifest.businessName}")
                    Text("Jenis: ${item.manifest.businessType}")
                    Text("Dibuat: ${formatDate(item.manifest.createdAt)}")
                    Text("Data aktif akan diamankan dulu sebelum diganti.")
                }
            },
            confirmButton = {
                Button(onClick = viewModel::confirmRestore) { Text("Restore sekarang") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestore) { Text("Batal") }
            },
        )
    }
}

@Composable
private fun ActionRow(vararg actions: Pair<String, () -> Unit>) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { (label, action) ->
            AssistChip(onClick = action, label = { Text(label) })
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoCard(title: String, subtitle: String) {
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
private fun ReportValue(label: String, value: String) {
    InfoCard(label, value)
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
    onSave: (Long, String, Long, Int, String) -> Unit,
) {
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
        title = { Text(if (product == null) "Tambah produk" else "Edit produk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (categories.isNotEmpty()) categoryIndex = (categoryIndex + 1) % categories.size
                    },
                ) {
                    Text("Kategori: ${categories.getOrNull(categoryIndex)?.second ?: "buat kategori dulu"}")
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
    onDismiss: () -> Unit,
    onSave: (Int, String, String) -> Unit,
) {
    val types = listOf("ADJUSTMENT_IN", "ADJUSTMENT_OUT", "DAMAGED", "LOST")
    var typeIndex by remember { mutableIntStateOf(0) }
    var quantity by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Penyesuaian stok") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                onSave(delta, types[typeIndex], reason)
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
    onDismiss: () -> Unit,
    onSave: (Long, Long, Int, Long, Long, String) -> Unit,
) {
    var supplierIndex by remember { mutableIntStateOf(0) }
    var productIndex by remember { mutableIntStateOf(0) }
    var values by remember { mutableStateOf(listOf("", "", "", "")) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Catat pembelian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionButton("Supplier", suppliers, supplierIndex) {
                    supplierIndex = nextIndex(supplierIndex, suppliers.size)
                }
                SelectionButton("Produk", products, productIndex) {
                    productIndex = nextIndex(productIndex, products.size)
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
                }) { Text(if (scheme == WorkerScheme.DAILY) "Pekerja harian" else "Freelancer/panggilan") }
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
    SimpleDateFormat("dd MMM yyyy HH:mm", Locale("id", "ID")).format(Date(timestamp))

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
