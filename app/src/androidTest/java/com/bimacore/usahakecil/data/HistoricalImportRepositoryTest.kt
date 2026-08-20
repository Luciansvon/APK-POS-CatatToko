package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.historyimport.HISTORY_IMPORT_SCHEMA_V1
import com.bimacore.usahakecil.historyimport.HistoryImportDraft
import com.bimacore.usahakecil.historyimport.HistoryImportItem
import com.bimacore.usahakecil.historyimport.HistoryImportPayload
import com.bimacore.usahakecil.historyimport.HistoryImportParser
import com.bimacore.usahakecil.historyimport.HistoryImportRecord
import com.bimacore.usahakecil.historyimport.HistoryImportReviewStatus
import com.bimacore.usahakecil.historyimport.HistoryImportSource
import com.bimacore.usahakecil.historyimport.HistoryImportSummary
import com.bimacore.usahakecil.security.ReportSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoricalImportRepositoryTest {
    private lateinit var database: PosDatabase
    private lateinit var session: ReportSession
    private lateinit var repository: HistoricalImportRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        session = ReportSession().also(ReportSession::unlock)
        repository = HistoricalImportRepository(database, BusinessType.RETAIL, session)
    }

    @After
    fun close() {
        database.close()
    }

    @Test
    fun import_sale_preserves_active_stock_and_historical_time() = runBlocking {
        database.catalogDao().insertProduct(
            ProductEntity(
                id = 7,
                categoryId = 1,
                name = "Dimsum Mentai",
                basePrice = 10_000,
                stock = 9,
                stockTrackingEnabled = true,
                hasVariants = false,
                lowStockThreshold = 2,
                imageUri = null,
                sortOrder = 1,
                unitLabel = "porsi",
            ),
        )
        val eventAt = 1_722_504_000_000L
        val result = repository.commit(draft(eventAt))

        assertEquals(1, result.appliedCount)
        assertEquals(1, result.archivedCount)
        assertEquals(9, database.catalogDao().getProduct(7)?.stock)
        val sale = database.saleDao().getSalesBetween(eventAt, eventAt).single()
        assertEquals(20_000L, sale.total)
        assertNull(sale.shiftId)
        assertEquals(eventAt, sale.createdAt)
        assertEquals(0, database.stockDao().getMovementsForSale(sale.id).size)
        assertEquals(1, database.operationsDao().getCashEntriesBetween(eventAt, eventAt).size)
        assertEquals(1, database.historyImportDao().batchCount())
        assertEquals(1, database.historyImportDao().recordCount(result.batchId, "IMPORTED"))
        assertEquals(1, database.historyImportDao().recordCount(result.batchId, "UNRESOLVED"))
    }

    @Test
    fun same_content_cannot_be_committed_twice() = runBlocking {
        val value = draft(1_722_504_000_000L)
        repository.commit(value)

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.commit(value) }
        }
        assertEquals(1, database.historyImportDao().batchCount())
    }

    @Test
    fun crafted_ready_cash_sale_with_zero_payment_is_rejected() = runBlocking {
        val valid = draft(1_722_504_000_000L)
        val saleRow = valid.rows.first()
        val invalidRecord = saleRow.record.copy(amountPaid = 0)
        val crafted = valid.copy(
            rows = listOf(saleRow.copy(record = invalidRecord)),
            contentHash = "crafted-invalid-payment",
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.commit(crafted) }
        }
        assertEquals(0, database.historyImportDao().batchCount())
        assertEquals(0, database.saleDao().getSalesBetween(0, Long.MAX_VALUE).size)
    }

    @Test
    fun crafted_ready_cash_entry_with_credit_method_is_rejected() = runBlocking {
        val sale = draft(1_722_504_000_000L).payload.records.first()
        val cash = sale.copy(
            sourceRef = "hal-1-kas-1",
            type = "CASH_IN",
            partyName = null,
            category = "Modal",
            paymentMethod = "CREDIT",
            amount = 10_000,
            amountPaid = null,
            items = emptyList(),
            rawText = "modal 10.000",
        )
        val payload = HistoryImportPayload(
            schemaVersion = HISTORY_IMPORT_SCHEMA_V1,
            source = HistoryImportSource("Buku kas", 1, "RETAIL", "Asia/Jakarta"),
            records = listOf(cash),
            summary = HistoryImportSummary(1, 0, 1, "2024-08-02", "2024-08-02", emptyList()),
        )
        val checked = HistoryImportParser().parse(
            text = Json.encodeToString(payload),
            expectedBusinessType = BusinessType.RETAIL,
            contentHash = "crafted-credit-cash",
        )
        val crafted = checked.copy(
            rows = listOf(
                checked.rows.single().copy(
                    status = HistoryImportReviewStatus.READY,
                    canApprove = false,
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.commit(crafted) }
        }
        assertEquals(0, database.historyImportDao().batchCount())
        assertEquals(0, database.operationsDao().getCashEntriesBetween(0, Long.MAX_VALUE).size)
    }

    private fun draft(eventAt: Long): HistoryImportDraft {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val eventDate = dateFormat.format(Date(eventAt))
        val eventTime = timeFormat.format(Date(eventAt))
        val sale = HistoryImportRecord(
            sourceRef = "hal-1-baris-1",
            type = "SALE",
            date = eventDate,
            time = eventTime,
            partyName = null,
            category = "Penjualan",
            paymentMethod = "CASH",
            amount = 20_000,
            amountPaid = 20_000,
            items = listOf(
                HistoryImportItem(
                    productName = "Dimsum Mentai",
                    variantName = null,
                    quantity = 2,
                    unitLabel = "porsi",
                    unitPrice = 10_000,
                    subtotal = 20_000,
                    uncertainFields = emptyList(),
                ),
            ),
            stockDelta = null,
            note = "Catatan lama",
            rawText = "2 dimsum mentai 20.000",
            uncertainFields = emptyList(),
            warnings = emptyList(),
        )
        val stock = sale.copy(
            sourceRef = "hal-1-baris-2",
            type = "STOCK_ADJUSTMENT",
            amount = null,
            amountPaid = null,
            items = emptyList(),
            stockDelta = 3,
            rawText = "stok tambah 3",
        )
        val payload = HistoryImportPayload(
            schemaVersion = HISTORY_IMPORT_SCHEMA_V1,
            source = HistoryImportSource("Buku lama", 1, "RETAIL", "Asia/Jakarta"),
            records = listOf(sale, stock),
            summary = HistoryImportSummary(2, 1, 1, eventDate, eventDate, emptyList()),
        )
        return HistoryImportParser().parse(
            text = Json.encodeToString(payload),
            expectedBusinessType = BusinessType.RETAIL,
            contentHash = "content-hash-1",
        )
    }
}
