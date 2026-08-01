package com.bimacore.usahakecil.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.bimacore.usahakecil.data.PosDatabase
import com.bimacore.usahakecil.security.ReportSession
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExcelExportManager(
    private val context: Context,
    private val database: PosDatabase,
    private val ownerSession: ReportSession,
    private val businessType: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun createExport(): Uri = withContext(Dispatchers.IO) {
        ownerSession.requireOwner()
        val profile = requireNotNull(database.profileDao().getProfile()) {
            "Profil usaha belum tersedia"
        }
        val exportedAt = clock()
        val workbook = collectWorkbook(profile.businessName, exportedAt)
        val directory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        val output = File(
            directory,
            "usaha-kecil-${safeFileName(profile.businessName)}-$exportedAt.xlsx",
        )
        FileOutputStream(output).use { stream ->
            ExcelWorkbookExporter.write(workbook, stream)
        }
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output,
        )
    }

    private fun collectWorkbook(businessName: String, exportedAt: Long): ExcelWorkbook = ExcelWorkbook(
        sheets = listOf(
            infoSheet(businessName, exportedAt),
            summarySheet(businessName, exportedAt),
            salesSheet(businessName, exportedAt),
            saleDetailsSheet(businessName, exportedAt),
            productSalesSheet(businessName, exportedAt),
            catalogSheet(businessName, exportedAt),
            purchaseSheet(businessName, exportedAt),
            purchaseDetailsSheet(businessName, exportedAt),
            cashSheet(businessName, exportedAt),
            debtSheet(businessName, exportedAt),
            debtPaymentSheet(businessName, exportedAt),
            shiftSheet(businessName, exportedAt),
            stockSheet(businessName, exportedAt),
            workforceSheet(businessName, exportedAt),
            attendanceSheet(businessName, exportedAt),
            freelanceSheet(businessName, exportedAt),
            workerPaymentSheet(businessName, exportedAt),
            wholesaleSheet(businessName, exportedAt),
            culinarySheet(businessName, exportedAt),
            partySheet(businessName, exportedAt),
        ),
    )

    private fun infoSheet(businessName: String, exportedAt: Long): ExcelSheet = ExcelSheet(
        name = "Info Export",
        rows = listOf(
            listOf("Kolom", "Nilai"),
            listOf("Nama usaha", businessName),
            listOf("Jenis usaha", businessType),
            listOf("Waktu export", formatExportTime(exportedAt)),
            listOf("Periode", "Seluruh data yang tersimpan offline di perangkat ini"),
            listOf("Format", "Laporan Excel terstruktur untuk dibaca Owner"),
            listOf("Catatan", "Export tidak mengubah transaksi, stok, atau histori keuangan"),
        ),
    )

    private fun summarySheet(businessName: String, exportedAt: Long): ExcelSheet {
        val sales = scalarPair("SELECT COUNT(*), COALESCE(SUM(total), 0) FROM sales")
        val cashIn = scalarLong(
            "SELECT COALESCE(SUM(amount), 0) FROM cash_entries WHERE type IN ('SALE_IN', 'CASH_IN', 'RECEIVABLE_IN')",
        )
        val cashOut = scalarLong(
            "SELECT COALESCE(SUM(amount), 0) FROM cash_entries WHERE type IN ('PURCHASE_OUT', 'CASH_OUT', 'EXPENSE', 'PAYABLE_OUT', 'WAGE_OUT')",
        )
        val expenses = scalarLong("SELECT COALESCE(SUM(amount), 0) FROM cash_entries WHERE type = 'EXPENSE'")
        val payments = queryRows(
            """
            SELECT paymentMethod, COUNT(*), COALESCE(SUM(
                CASE WHEN paymentMethod = 'CASH' THEN total ELSE amountReceived END
            ), 0)
            FROM sales GROUP BY paymentMethod ORDER BY paymentMethod
            """.trimIndent(),
        )
        val rows = mutableListOf(
            listOf("Laporan Operasional - $businessName"),
            listOf("Jenis usaha: $businessType"),
            listOf("Waktu export: ${formatExportTime(exportedAt)}"),
            listOf("Periode: seluruh data yang tersimpan offline di perangkat ini"),
            emptyList(),
            listOf("Ringkasan Keuangan", "Nilai"),
            listOf("Jumlah transaksi", sales.first.toString()),
            listOf("Penjualan", formatRupiah(sales.second)),
            listOf("Kas masuk", formatRupiah(cashIn)),
            listOf("Kas keluar", formatRupiah(cashOut)),
            listOf("Pengeluaran operasional", formatRupiah(expenses)),
            listOf("Saldo kas tercatat", formatRupiah(cashIn - cashOut)),
            listOf("Sisa utang", formatRupiah(outstandingDebt("PAYABLE"))),
            listOf("Sisa piutang", formatRupiah(outstandingDebt("RECEIVABLE"))),
            emptyList(),
            listOf("Metode Pembayaran", "Transaksi", "Total"),
        )
        rows += payments.map { payment ->
            listOf(payment[0], payment[1], formatRupiah(payment[2].toLong()))
        }
        return ExcelSheet(
            name = "Ringkasan",
            rows = rows,
            headerRows = setOf(5, 15),
            titleRows = setOf(0),
            subtitleRows = setOf(1, 2, 3),
        )
    }

    private fun salesSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Penjualan",
        title = "Laporan Penjualan - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf(
            "No", "Nomor Struk", "Tanggal", "Metode Pembayaran", "Status Bayar",
            "Status Pesanan", "Total", "Diterima", "Kembalian", "Pelanggan", "Kasir",
        ),
        rows = queryRows(
            """
            SELECT s.receiptNumber, s.createdAt, s.paymentMethod, s.settlementStatus, s.orderStatus,
                   s.total, s.amountReceived, s.changeAmount, COALESCE(c.name, ''),
                   COALESCE(sh.cashierName, '')
            FROM sales s
            LEFT JOIN parties c ON c.id = s.customerId
            LEFT JOIN shifts sh ON sh.id = s.shiftId
            ORDER BY s.createdAt DESC, s.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], formatDateTime(row[1].toLongOrNull()),
                paymentLabel(row[2]), settlementLabel(row[3]), orderStatusLabel(row[4]),
                formatRupiah(row[5].toLong()), formatRupiah(row[6].toLong()),
                formatRupiah(row[7].toLong()), row[8].ifBlank { "-" }, row[9].ifBlank { "-" },
            )
        },
    )

    private fun saleDetailsSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Detail Penjualan",
        title = "Detail Penjualan - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf(
            "No", "Nomor Struk", "Tanggal", "Produk", "Varian", "Kategori", "Satuan",
            "Qty", "Qty Dasar", "Harga Satuan", "Subtotal", "Topping", "Catatan",
        ),
        rows = queryRows(
            """
            SELECT s.receiptNumber, s.createdAt, i.productName, COALESCE(i.variantName, ''),
                   i.categoryName, i.unitLabel, i.quantity, i.baseQuantity, i.unitPrice,
                   i.subtotal,
                   COALESCE((SELECT GROUP_CONCAT(toppingName || ' x' || quantity, ', ')
                             FROM sale_item_toppings WHERE saleItemId = i.id), ''),
                   i.note
            FROM sale_items i INNER JOIN sales s ON s.id = i.saleId
            ORDER BY s.createdAt DESC, i.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], formatDateTime(row[1].toLongOrNull()), row[2],
                row[3].ifBlank { "-" }, row[4], row[5], row[6], row[7], formatRupiah(row[8].toLong()),
                formatRupiah(row[9].toLong()), row[10].ifBlank { "-" }, row[11].ifBlank { "-" },
            )
        },
    )

    private fun productSalesSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Produk Terjual",
        title = "Rekap Produk Terjual - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf(
            "No", "Produk", "Varian", "Kategori", "Satuan", "Qty Jual", "Qty Dasar",
            "Omzet", "Jumlah Transaksi",
        ),
        rows = queryRows(
            """
            SELECT productName, COALESCE(variantName, ''), categoryName, unitLabel,
                   COALESCE(SUM(quantity), 0), COALESCE(SUM(baseQuantity), 0),
                   COALESCE(SUM(subtotal), 0), COUNT(DISTINCT saleId)
            FROM sale_items GROUP BY productName, variantName, categoryName, unitLabel
            ORDER BY SUM(subtotal) DESC, productName
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], row[1].ifBlank { "-" }, row[2], row[3],
                row[4], row[5], formatRupiah(row[6].toLong()), row[7],
            )
        },
    )

    private fun catalogSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Katalog & Stok",
        title = "Katalog dan Stok - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf(
            "No", "Kategori", "Produk", "Tipe", "Varian", "Satuan", "Harga Jual",
            "Stok", "Batas Stok", "Status",
        ),
        rows = queryRows(
            """
            SELECT COALESCE(c.name, 'Lainnya'), p.name, 'Produk', '', p.unitLabel, p.basePrice,
                   p.stock, p.lowStockThreshold, CASE WHEN p.isActive = 1 THEN 'Aktif' ELSE 'Nonaktif' END
            FROM products p LEFT JOIN categories c ON c.id = p.categoryId
            UNION ALL
            SELECT COALESCE(c.name, 'Lainnya'), p.name, 'Varian', v.label, p.unitLabel,
                   COALESCE(v.priceOverride, p.basePrice), v.stock, '',
                   CASE WHEN v.isActive = 1 THEN 'Aktif' ELSE 'Nonaktif' END
            FROM product_variants v INNER JOIN products p ON p.id = v.productId
            LEFT JOIN categories c ON c.id = p.categoryId
            ORDER BY 2, 3, 4
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], row[1], row[2], row[3].ifBlank { "-" }, row[4],
                formatRupiah(row[5].toLong()), row[6], row[7].ifBlank { "-" }, row[8],
            )
        },
    )

    private fun purchaseSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Pembelian",
        title = "Pembelian Supplier - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Supplier", "No Faktur", "Total", "Dibayar", "Status", "Catatan"),
        rows = queryRows(
            "SELECT createdAt, supplierName, invoiceNumber, total, amountPaid, settlementStatus, note FROM purchases ORDER BY createdAt DESC, id DESC",
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), row[1], row[2],
                formatRupiah(row[3].toLong()), formatRupiah(row[4].toLong()), settlementLabel(row[5]),
                row[6].ifBlank { "-" },
            )
        },
    )

    private fun purchaseDetailsSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Detail Pembelian",
        title = "Detail Pembelian - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Supplier", "No Faktur", "Produk", "Varian", "Satuan", "Qty", "Qty Dasar", "Harga Modal", "Subtotal"),
        rows = queryRows(
            """
            SELECT p.supplierName, p.invoiceNumber, i.productName, COALESCE(i.variantName, ''),
                   i.unitLabel, i.quantity, i.baseQuantity, i.unitCost, i.subtotal
            FROM purchase_items i INNER JOIN purchases p ON p.id = i.purchaseId
            ORDER BY p.createdAt DESC, i.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], row[1], row[2], row[3].ifBlank { "-" }, row[4],
                row[5], row[6], formatRupiah(row[7].toLong()), formatRupiah(row[8].toLong()),
            )
        },
    )

    private fun cashSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Kas",
        title = "Kas Masuk dan Keluar - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Jenis", "Kategori", "Nominal", "Metode", "Referensi", "Catatan", "Shift"),
        rows = queryRows(
            "SELECT createdAt, type, category, amount, paymentMethod, COALESCE(referenceType, ''), note, COALESCE(shiftId, '') FROM cash_entries ORDER BY createdAt DESC, id DESC",
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), cashTypeLabel(row[1]), row[2],
                formatRupiah(row[3].toLong()), paymentLabel(row[4]), row[5].ifBlank { "-" },
                row[6].ifBlank { "-" }, row[7].ifBlank { "-" },
            )
        },
    )

    private fun debtSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Utang Piutang",
        title = "Utang dan Piutang - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Jenis", "Pihak", "Total Awal", "Dibayar", "Sisa", "Status", "Tanggal", "Catatan"),
        rows = queryRows(
            "SELECT kind, partyName, originalAmount, paidAmount, settlementStatus, createdAt, note FROM debts ORDER BY createdAt DESC, id DESC",
        ).mapIndexed { index, row ->
            val original = row[2].toLong()
            val paid = row[3].toLong()
            listOf(
                (index + 1).toString(), debtKindLabel(row[0]), row[1], formatRupiah(original),
                formatRupiah(paid), formatRupiah((original - paid).coerceAtLeast(0)), settlementLabel(row[4]),
                formatDateTime(row[5].toLongOrNull()), row[6].ifBlank { "-" },
            )
        },
    )

    private fun debtPaymentSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Pembayaran Utang",
        title = "Riwayat Pembayaran Utang/Piutang - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Jenis", "Pihak", "Nominal", "Metode", "Catatan"),
        rows = queryRows(
            """
            SELECT p.paidAt, d.kind, d.partyName, p.amount, p.paymentMethod, p.note
            FROM debt_payments p INNER JOIN debts d ON d.id = p.debtId
            ORDER BY p.paidAt DESC, p.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), debtKindLabel(row[1]), row[2],
                formatRupiah(row[3].toLong()), paymentLabel(row[4]), row[5].ifBlank { "-" },
            )
        },
    )

    private fun shiftSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Shift",
        title = "Riwayat Shift - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf(
            "No", "Kasir", "Buka", "Tutup", "Status", "Modal Awal", "Total Penjualan",
            "Tunai", "Non Tunai", "Kas Seharusnya", "Kas Akhir", "Selisih",
        ),
        rows = queryRows(
            "SELECT cashierName, openedAt, closedAt, status, openingCash, totalSales, cashSales, nonCashSales, expectedCash, closingCash, cashDifference FROM shifts ORDER BY openedAt DESC, id DESC",
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], formatDateTime(row[1].toLongOrNull()),
                formatDateTime(row[2].toLongOrNull()), row[3], formatRupiah(row[4].toLong()),
                formatRupiah(row[5].toLong()), formatRupiah(row[6].toLong()), formatRupiah(row[7].toLong()),
                formatRupiah(row[8].toLong()), formatRupiah(row[9].toLong()), formatRupiah(row[10].toLong()),
            )
        },
    )

    private fun stockSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Pergerakan Stok",
        title = "Pergerakan Stok - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Produk", "Varian", "Jenis", "Perubahan", "Perubahan Dasar", "Satuan", "Alasan", "Referensi"),
        rows = queryRows(
            """
            SELECT sm.createdAt, COALESCE(p.name, 'Produk #' || sm.productId), COALESCE(v.label, ''),
                   sm.type, sm.quantityDelta, sm.baseQuantityDelta, sm.unitLabel, sm.reason, sm.referenceType
            FROM stock_movements sm
            LEFT JOIN products p ON p.id = sm.productId
            LEFT JOIN product_variants v ON v.id = sm.variantId
            ORDER BY sm.createdAt DESC, sm.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), row[1], row[2].ifBlank { "-" },
                row[3], row[4], row[5], row[6], row[7], row[8],
            )
        },
    )

    private fun workforceSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Tenaga Kerja",
        title = "Tenaga Kerja - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Nama", "Skema", "Telepon", "Status", "Tarif Terakhir", "Tanggal Tarif"),
        rows = queryRows(
            """
            SELECT e.name, e.scheme, e.phone, CASE WHEN e.isActive = 1 THEN 'Aktif' ELSE 'Nonaktif' END,
                   COALESCE((SELECT amount FROM wage_rates w WHERE w.employeeId = e.id ORDER BY effectiveAt DESC LIMIT 1), 0),
                   COALESCE((SELECT effectiveAt FROM wage_rates w WHERE w.employeeId = e.id ORDER BY effectiveAt DESC LIMIT 1), '')
            FROM employees e ORDER BY e.isActive DESC, e.name
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), row[0], schemeLabel(row[1]), row[2].ifBlank { "-" }, row[3],
                formatRupiah(row[4].toLong()), formatDateTime(row[5].toLongOrNull()),
            )
        },
    )

    private fun attendanceSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Kehadiran",
        title = "Kehadiran dan Upah Harian - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Pekerja", "Status", "Tarif", "Lembur", "Bonus", "Potongan", "Kasbon", "Upah Bersih", "Dibayar", "Catatan"),
        rows = queryRows(
            """
            SELECT a.workDate, e.name, a.status, a.rateSnapshot, a.overtime, a.bonus, a.deduction,
                   a.advance, a.netPay, CASE WHEN a.isPaid = 1 THEN 'Sudah dibayar' ELSE 'Belum dibayar' END, a.note
            FROM attendance_records a INNER JOIN employees e ON e.id = a.employeeId
            ORDER BY a.workDate DESC, a.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), row[1], row[2],
                formatRupiah(row[3].toLong()), formatRupiah(row[4].toLong()), formatRupiah(row[5].toLong()),
                formatRupiah(row[6].toLong()), formatRupiah(row[7].toLong()), formatRupiah(row[8].toLong()),
                row[9], row[10].ifBlank { "-" },
            )
        },
    )

    private fun freelanceSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Freelancer",
        title = "Pekerjaan Freelancer/Panggilan - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Pekerja", "Pekerjaan", "Kesepakatan", "Dibayar", "Status", "Catatan"),
        rows = queryRows(
            """
            SELECT j.workDate, e.name, j.title, j.agreedAmount, j.paidAmount, j.status, j.note
            FROM freelance_jobs j INNER JOIN employees e ON e.id = j.employeeId
            ORDER BY j.workDate DESC, j.id DESC
            """.trimIndent(),
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), row[1], row[2],
                formatRupiah(row[3].toLong()), formatRupiah(row[4].toLong()), settlementLabel(row[5]),
                row[6].ifBlank { "-" },
            )
        },
    )

    private fun workerPaymentSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Pembayaran Pekerja",
        title = "Pembayaran Pekerja - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Tanggal", "Pekerja", "Referensi", "Nominal", "Catatan"),
        rows = queryRows(
            "SELECT p.paidAt, e.name, p.referenceType, p.amount, p.note FROM worker_payments p INNER JOIN employees e ON e.id = p.employeeId ORDER BY p.paidAt DESC, p.id DESC",
        ).mapIndexed { index, row ->
            listOf(
                (index + 1).toString(), formatDateTime(row[0].toLongOrNull()), row[1], row[2],
                formatRupiah(row[3].toLong()), row[4].ifBlank { "-" },
            )
        },
    )

    private fun wholesaleSheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Grosir",
        title = "Satuan dan Harga Bertingkat - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("Jenis", "Produk ID", "Label", "Faktor ke Dasar", "Minimum Qty Dasar", "Harga", "Status"),
        rows = queryRows(
            "SELECT 'Konversi Satuan', productId, label, factorToBase, '', salePrice, CASE WHEN isActive = 1 THEN 'Aktif' ELSE 'Nonaktif' END FROM unit_conversions UNION ALL SELECT 'Harga Bertingkat', productId, '', '', minimumBaseQuantity, unitPrice, 'Aktif' FROM price_tiers ORDER BY productId, 1, 3",
        ).map { row ->
            listOf(
                row[0], row[1], row[2].ifBlank { "-" }, row[3].ifBlank { "-" }, row[4].ifBlank { "-" },
                formatRupiah(row[5].toLong()), row[6],
            )
        },
    )

    private fun culinarySheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Kuliner",
        title = "Topping dan Resep - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("Jenis", "Menu/Produk ID", "Nama/Ingredient ID", "Qty per Menu", "Harga", "Status"),
        rows = queryRows(
            "SELECT 'Topping', productId, label, '', price, CASE WHEN isActive = 1 THEN 'Aktif' ELSE 'Nonaktif' END FROM toppings UNION ALL SELECT 'Bahan Resep', menuProductId, ingredientProductId, quantityPerMenu, '', 'Aktif' FROM recipe_ingredients ORDER BY 2, 1, 3",
        ).map { row ->
            listOf(
                row[0], row[1], row[2], row[3].ifBlank { "-" },
                row[4].ifBlank { "-" }.toLongOrNull()?.let(::formatRupiah) ?: row[4].ifBlank { "-" }, row[5],
            )
        },
    )

    private fun partySheet(businessName: String, exportedAt: Long): ExcelSheet = reportSheet(
        name = "Pelanggan & Supplier",
        title = "Pelanggan dan Supplier - $businessName",
        businessName = businessName,
        exportedAt = exportedAt,
        headers = listOf("No", "Jenis", "Nama", "Telepon", "Alamat", "Status"),
        rows = queryRows(
            "SELECT kind, name, phone, address, CASE WHEN isActive = 1 THEN 'Aktif' ELSE 'Nonaktif' END FROM parties ORDER BY kind, isActive DESC, name",
        ).mapIndexed { index, row ->
            listOf((index + 1).toString(), partyKindLabel(row[0]), row[1], row[2].ifBlank { "-" }, row[3].ifBlank { "-" }, row[4])
        },
    )

    private fun reportSheet(
        name: String,
        title: String,
        businessName: String,
        exportedAt: Long,
        headers: List<String>,
        rows: List<List<String>>,
    ): ExcelSheet = ExcelSheet(
        name = name,
        rows = buildList {
            add(listOf(title))
            add(listOf("Usaha: $businessName"))
            add(listOf("Export: ${formatExportTime(exportedAt)}"))
            add(emptyList())
            add(headers)
            addAll(rows)
        },
        headerRows = setOf(4),
        titleRows = setOf(0),
        subtitleRows = setOf(1, 2),
    )

    private fun queryRows(sql: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        database.openHelper.readableDatabase.query(sql).use { cursor ->
            while (cursor.moveToNext()) {
                rows += List(cursor.columnCount) { index ->
                    if (cursor.isNull(index)) "" else cursor.getString(index)
                }
            }
        }
        return rows
    }

    private fun scalarPair(sql: String): Pair<Long, Long> =
        queryRows(sql).firstOrNull()?.let { row ->
            (row.getOrNull(0)?.toLongOrNull() ?: 0L) to (row.getOrNull(1)?.toLongOrNull() ?: 0L)
        } ?: (0L to 0L)

    private fun scalarLong(sql: String): Long =
        queryRows(sql).firstOrNull()?.firstOrNull()?.toLongOrNull() ?: 0L

    private fun outstandingDebt(kind: String): Long = scalarLong(
        "SELECT COALESCE(SUM(originalAmount - paidAmount), 0) FROM debts WHERE kind = '$kind' AND settlementStatus != 'PAID'",
    ).coerceAtLeast(0L)

    private fun formatExportTime(timestamp: Long): String = SimpleDateFormat(
        "dd MMMM yyyy, HH:mm:ss z",
        Locale.forLanguageTag("id-ID"),
    ).format(Date(timestamp))

    private fun formatDateTime(timestamp: Long?): String = timestamp?.let {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID")).format(Date(it))
    }.orEmpty().ifBlank { "-" }

    private fun formatRupiah(value: Long): String =
        "Rp ${NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)}"

    private fun paymentLabel(value: String): String = when (value) {
        "CASH" -> "Tunai"
        "QRIS" -> "QRIS"
        "TRANSFER" -> "Transfer"
        "CREDIT" -> "Piutang"
        else -> value
    }

    private fun settlementLabel(value: String): String = when (value) {
        "PAID" -> "Lunas"
        "PARTIAL" -> "Sebagian"
        "UNPAID" -> "Belum dibayar"
        else -> value
    }

    private fun orderStatusLabel(value: String): String = when (value) {
        "NEW" -> "Baru"
        "PROCESSING" -> "Diproses"
        "READY" -> "Siap"
        "COMPLETED" -> "Selesai"
        else -> value
    }

    private fun cashTypeLabel(value: String): String = when (value) {
        "SALE_IN" -> "Penjualan"
        "CASH_IN" -> "Kas masuk"
        "RECEIVABLE_IN" -> "Bayar piutang"
        "PURCHASE_OUT" -> "Pembelian"
        "CASH_OUT" -> "Kas keluar"
        "EXPENSE" -> "Pengeluaran"
        "PAYABLE_OUT" -> "Bayar utang"
        "WAGE_OUT" -> "Upah"
        else -> value
    }

    private fun debtKindLabel(value: String): String = when (value) {
        "PAYABLE" -> "Utang"
        "RECEIVABLE" -> "Piutang"
        else -> value
    }

    private fun partyKindLabel(value: String): String = when (value) {
        "CUSTOMER" -> "Pelanggan"
        "SUPPLIER" -> "Supplier"
        else -> value
    }

    private fun schemeLabel(value: String): String = when (value) {
        "DAILY" -> "Harian"
        "FREELANCE" -> "Freelancer"
        else -> value
    }

    private fun safeFileName(value: String): String = value
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "usaha" }
        .take(40)

    private companion object {
        const val EXPORT_DIRECTORY = "excel-exports"
    }
}
