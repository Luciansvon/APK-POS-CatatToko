package com.bimacore.usahakecil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.data.ShiftEntity
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun lockOwnerSessionForTestIsolation() {
        val application = composeRule.activity.application as PosApplication
        runBlocking {
            application.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM report_security",
            )
        }
        (composeRule.activity.application as PosApplication).reportSession.lock()
        composeRule.activity.runOnUiThread {
            composeRule.activity.viewModelStore.clear()
            composeRule.activity.recreate()
        }
        composeRule.waitForIdle()
        dismissFirstRunGuideIfPresent()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("start-transaction")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun first_run_guide_requires_confirmation_and_explains_both_modes() {
        composeRule.activity
            .getSharedPreferences(FirstRunGuidePreferences.FILE_NAME, 0)
            .edit()
            .clear()
            .commit()
        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("first-run-guide")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Mode Kasir / Pekerja").assertIsDisplayed()
        composeRule.onNodeWithText("Mode Owner").assertIsDisplayed()
        composeRule.onNodeWithText("Buka Mode Owner", substring = true).assertIsDisplayed()
        composeRule.onAllNodesWithText("Lewati").assertCountEquals(0)
        composeRule.onNodeWithTag("onboarding-complete").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.activity
                .getSharedPreferences(FirstRunGuidePreferences.FILE_NAME, 0)
                .getBoolean(FirstRunGuidePreferences.COMPLETED_KEY, false)
        }
        composeRule.waitForIdle()
        assertTrue(
            composeRule.activity
                .getSharedPreferences(FirstRunGuidePreferences.FILE_NAME, 0)
                .getBoolean(FirstRunGuidePreferences.COMPLETED_KEY, false),
        )
    }

    @Test
    fun catalog_and_calculator_are_accessible() {
        ensureOpenShift()
        composeRule.onNodeWithTag("catattoko-brand").assertExists()
        composeRule.onNodeWithText("CatatToko").assertIsDisplayed()
        composeRule.onNodeWithTag("start-transaction").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Pilih Produk")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("Pilih Produk")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Buka kalkulator").performClick()
        composeRule.onNodeWithText("Kalkulator").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tutup kalkulator").performClick()
    }

    @Test
    fun start_transaction_without_shift_prompts_to_open_shift() {
        clearOpenShift()
        composeRule.onNodeWithTag("start-transaction").performScrollTo().performClick()
        composeRule.onNodeWithText("Nama kasir").assertIsDisplayed()
        composeRule.onNodeWithText("Modal awal").assertIsDisplayed()
        composeRule.onNodeWithText("Batal").performClick()
        composeRule.onNodeWithText("Pilih Produk").assertDoesNotExist()
    }

    @Test
    fun owner_can_start_cashier_without_opening_a_shift() {
        assumeTrue(BuildConfig.BUSINESS_TYPE == "RETAIL")
        clearOpenShift()
        val application = composeRule.activity.application as PosApplication
        composeRule.activity.runOnUiThread { application.reportSession.unlock() }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Laporan")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-transaction").performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start-transaction").performClick()
        waitForText("Pilih Produk")
        composeRule.onNodeWithTag("product-101").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("continue-payment").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("cart-summary").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithTag("cart-summary").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("cart-summary").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("continue-payment").assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("continue-payment").performClick()
        waitForText("Pembayaran")
        composeRule.onNodeWithTag("payment-list")
            .performScrollToNode(hasTestTag("quick-cash-option"))
        val quickCashOptions = composeRule.onAllNodesWithTag("quick-cash-option")
        quickCashOptions[quickCashOptions.fetchSemanticsNodes().lastIndex].performClick()
        composeRule.onNodeWithTag("complete-sale").performClick()
        waitForText("Transaksi Berhasil")
        composeRule.onNodeWithTag("receipt-list")
            .performScrollToNode(hasTestTag("new-transaction"))
        composeRule.onNodeWithTag("new-transaction").performClick()
    }

    @Test
    fun cash_sale_shows_change_and_starts_clean_transaction() {
        assumeTrue(BuildConfig.BUSINESS_TYPE == "RETAIL")
        ensureOpenShift()
        composeRule.onNodeWithTag("start-transaction").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Keripik Singkong")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("product-101").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("continue-payment").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("cart-summary").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithTag("cart-summary").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("cart-summary").performClick()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("continue-payment").assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        composeRule.onNodeWithTag("continue-payment").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("payment-list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("payment-list")
            .performScrollToNode(hasTestTag("quick-cash-option"))
        val quickCashOptions = composeRule.onAllNodesWithTag("quick-cash-option")
        quickCashOptions[quickCashOptions.fetchSemanticsNodes().lastIndex].performClick()
        composeRule.onNodeWithTag("complete-sale").performClick()

        runCatching {
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Transaksi Berhasil")
                    .fetchSemanticsNodes().isNotEmpty()
            }
        }.getOrElse { error ->
            throw AssertionError(
                "Layar me-render:\n${composeRule.onRoot().printToString()}",
                error,
            )
        }
        composeRule.onNodeWithText("Transaksi Berhasil").assertIsDisplayed()
        composeRule.onAllNodesWithText("Kembalian")[0].assertIsDisplayed()
        composeRule.onNodeWithTag("receipt-list")
            .performScrollToNode(hasTestTag("new-transaction"))
        composeRule.onNodeWithTag("new-transaction").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Pilih Produk")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun flavor_navigation_only_shows_relevant_business_modules() {
        composeRule.onNodeWithText("Laporan").assertDoesNotExist()
        unlockOwner()

        val operationsLabel = when (BuildConfig.BUSINESS_TYPE) {
            "WHOLESALE" -> "Grosir"
            "CULINARY" -> "Pesanan"
            else -> "Operasional"
        }
        composeRule.onNodeWithText(operationsLabel).performClick()
        composeRule.onNodeWithTag("operations-section-grid").assertExists()
        composeRule.onNodeWithText("Pembelian").assertIsDisplayed()

        when (BuildConfig.BUSINESS_TYPE) {
            "WHOLESALE" -> {
                assertTrue(
                    composeRule.onAllNodesWithText("Grosir")
                        .fetchSemanticsNodes().isNotEmpty(),
                )
                composeRule.onAllNodesWithText("Kuliner").assertCountEquals(0)
            }
            "CULINARY" -> {
                assertTrue(
                    composeRule.onAllNodesWithText("Kuliner")
                        .fetchSemanticsNodes().isNotEmpty(),
                )
                composeRule.onAllNodesWithText("Grosir").assertCountEquals(0)
            }
            else -> {
                composeRule.onAllNodesWithText("Grosir").assertCountEquals(0)
                composeRule.onAllNodesWithText("Kuliner").assertCountEquals(0)
            }
        }
    }

    @Test
    fun product_form_exposes_menu_photo_picker() {
        unlockOwner()

        val operationsLabel = when (BuildConfig.BUSINESS_TYPE) {
            "WHOLESALE" -> "Grosir"
            "CULINARY" -> "Pesanan"
            else -> "Operasional"
        }
        composeRule.onNodeWithText(operationsLabel).performClick()
        if (BuildConfig.BUSINESS_TYPE != "RETAIL") {
            composeRule.onAllNodesWithText("Produk")[0].performClick()
        }
        waitForText("Tambah produk")
        composeRule.onNodeWithText("Tambah produk").performClick()
        composeRule.onNodeWithTag("product-image-picker").assertIsDisplayed()
        composeRule.onNodeWithText("Pilih foto menu").assertIsDisplayed()
    }

    @Test
    fun protected_reports_and_backup_are_reachable() {
        composeRule.onNodeWithText("Laporan").assertDoesNotExist()
        unlockOwner()
        composeRule.onNodeWithText("Laporan").performClick()
        composeRule.onNodeWithText("Omzet hari ini").assertIsDisplayed()
        composeRule.onNodeWithTag("report-period-selector").assertIsDisplayed()
        composeRule.onNodeWithTag("report-period-selector").performClick()
        listOf("DAY", "WEEK", "MONTH", "YEAR").forEach { period ->
            composeRule.onNodeWithTag("report-period-option-$period").assertIsDisplayed()
        }
        composeRule.onNodeWithTag("report-period-option-DAY").performClick()
        composeRule.onNodeWithTag("report-full-details")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("payment-method-chart").performScrollTo().assertIsDisplayed()
        listOf("Tunai", "QRIS", "Transfer", "Piutang").forEach { method ->
            val matches = composeRule.onAllNodesWithText(method)
            assertTrue(matches.fetchSemanticsNodes().isNotEmpty())
        }
        composeRule.onNodeWithTag("excel-export").performScrollTo().assertIsDisplayed()
        waitForEnabledTag("excel-export")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Simpan Laporan Excel").performClick()
        waitForText("Bagikan Excel", timeoutMillis = 30_000)

        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Salinan & keamanan data").assertIsDisplayed()
        composeRule.onNodeWithText("Buat salinan sekarang")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun owner_mode_does_not_require_an_open_shift_for_management_or_export() {
        val application = composeRule.activity.application as PosApplication
        runBlocking {
            application.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM shifts WHERE status = 'OPEN'",
            )
        }
        composeRule.waitForIdle()

        unlockOwner()
        composeRule.onNodeWithText("Laporan").performClick()
        waitForText("Omzet hari ini")
        composeRule.onNodeWithTag("excel-export").performScrollTo().assertIsDisplayed()
        waitForEnabledTag("excel-export")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("excel-export").performTouchInput { click() }
        waitForText("Bagikan Excel", timeoutMillis = 30_000)

        composeRule.onNodeWithText("Lainnya").performClick()
        waitForText("Salinan & keamanan data")
    }

    @Test
    fun owner_mode_covers_all_relevant_screens_and_locks_again() {
        composeRule.onNodeWithText("Laporan").assertDoesNotExist()
        unlockOwner()

        val operationsLabel = when (BuildConfig.BUSINESS_TYPE) {
            "WHOLESALE" -> "Grosir"
            "CULINARY" -> "Pesanan"
            else -> "Operasional"
        }
        val financeLabel = "Keuangan"

        composeRule.onNodeWithText(operationsLabel).performClick()
        when (BuildConfig.BUSINESS_TYPE) {
            "WHOLESALE" -> {
                waitForText("Multi-satuan dan harga bertingkat", substring = true)
                composeRule.onAllNodesWithText("Produk")[0].performClick()
            }

            "CULINARY" -> {
                waitForText("Atur topping/resep")
                composeRule.onAllNodesWithText("Produk")[0].performClick()
            }
        }
        waitForText("Tambah produk")
        composeRule.onAllNodesWithText("Stok")[0].performClick()
        waitForText("Stok perlu perhatian")
        composeRule.onAllNodesWithText("Pembelian")[0].performClick()
        waitForText("Catat pembelian")
        waitForText("Total pembelian tercatat")
        composeRule.onAllNodesWithText("Pekerja")[0].performClick()
        waitForText("Tambah pekerja")
        waitForText("Pembayaran pekerja tertunda")

        composeRule.onNodeWithText(financeLabel).performClick()
        if (BuildConfig.BUSINESS_TYPE != "RETAIL") {
            waitForText("Shift kasir")
        }
        composeRule.onAllNodesWithText("Utang & Piutang")[0].performClick()
        waitForText("Daftar utang & piutang")
        if (BuildConfig.BUSINESS_TYPE != "CULINARY") {
            waitForText("Tambah piutang")
            waitForText("Tambah pelanggan")
        }
        composeRule.onAllNodesWithText("Transaksi")[0].performClick()
        waitForText("Cari nomor struk")
        composeRule.onAllNodesWithText("Kas")[0].performClick()
        waitForText("Shift kasir")

        composeRule.onNodeWithText("Laporan").performClick()
        waitForText("Omzet hari ini")
        composeRule.onNodeWithTag("report-full-details")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForText("Perkiraan penjualan")
        composeRule.onNodeWithTag("excel-export").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithText("Lainnya").performClick()
        waitForText("Profil usaha")
        waitForText("Ubah nama usaha")
        waitForText("Salinan & keamanan data")
        waitForText("Buat salinan sekarang")
        waitForText("Pilih berkas salinan")
        waitForText("Ganti PIN Owner")
        waitForText("Keluar Mode Owner")
        composeRule.onNodeWithText("tanpa akun", substring = true).performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("owner-exit")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForText("Mode Kasir")
    }

    private fun unlockOwner() {
        composeRule.onNodeWithTag("owner-access").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("owner-pin-input")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("owner-pin-input").performTextInput("2468")
        composeRule.onNodeWithTag("owner-submit").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Laporan")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Laporan").performClick()
        waitForText("Omzet hari ini")
        composeRule.onNodeWithText("Kasir").performClick()
        composeRule.waitForIdle()
    }

    private fun dismissFirstRunGuideIfPresent() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("onboarding-complete").fetchSemanticsNodes().isNotEmpty() ||
                composeRule.onAllNodesWithTag("owner-access").fetchSemanticsNodes().isNotEmpty()
        }
        if (composeRule.onAllNodesWithTag("onboarding-complete").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-complete").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
    }

    private fun ensureOpenShift() {
        val application = composeRule.activity.application as PosApplication
        runBlocking {
            if (application.database.shiftDao().getOpenShift() == null) {
                application.database.shiftDao().insertShift(
                    ShiftEntity(
                        cashierName = "Kasir QA",
                        openedAt = System.currentTimeMillis(),
                        openingCash = 0,
                        openSlot = 1,
                    ),
                )
            }
        }
    }

    private fun clearOpenShift() {
        val application = composeRule.activity.application as PosApplication
        runBlocking {
            application.database.openHelper.writableDatabase.execSQL(
                "DELETE FROM shifts WHERE status = 'OPEN'",
            )
        }
        composeRule.waitForIdle()
    }

    private fun waitForText(
        text: String,
        substring: Boolean = false,
        timeoutMillis: Long = 5_000,
    ) {
        try {
            composeRule.waitUntil(timeoutMillis = timeoutMillis) {
                composeRule.onAllNodesWithText(text, substring = substring)
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
        } catch (error: Throwable) {
            throw AssertionError(
                "Gagal menunggu teks '$text'. Layar saat gagal:\n${composeRule.onRoot().printToString()}",
                error,
            )
        }
    }

    private fun waitForEnabledTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onNodeWithTag(tag).fetchSemanticsNode().config
                .contains(SemanticsProperties.Disabled).not()
        }
    }

}
