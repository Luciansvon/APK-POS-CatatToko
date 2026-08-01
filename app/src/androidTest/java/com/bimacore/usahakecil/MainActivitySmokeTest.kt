package com.bimacore.usahakecil

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertCountEquals
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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
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
        (composeRule.activity.application as PosApplication).reportSession.lock()
    }

    @Test
    fun catalog_and_calculator_are_accessible() {
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
            .performScrollToNode(hasTestTag("quick-cash-20000"))
        composeRule.onNodeWithTag("quick-cash-20000").performClick()
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
        composeRule.onAllNodesWithText("Rp8.000")[0].assertIsDisplayed()
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
    fun protected_reports_and_backup_are_reachable() {
        composeRule.onNodeWithText("Laporan").assertDoesNotExist()
        unlockOwner()
        composeRule.onNodeWithText("Laporan").performClick()
        composeRule.onNodeWithText("Omzet hari ini").assertIsDisplayed()
        composeRule.onNodeWithTag("payment-method-chart").assertExists()

        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Backup & restore").assertIsDisplayed()
        composeRule.onNodeWithText("Buat backup lokal").assertIsDisplayed()
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
        val financeLabel = if (BuildConfig.BUSINESS_TYPE == "RETAIL") "Piutang" else "Keuangan"

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
        waitForText("Riwayat terbaru")
        composeRule.onAllNodesWithText("Pembelian")[0].performClick()
        waitForText("Catat pembelian")
        composeRule.onAllNodesWithText("Pekerja")[0].performClick()
        waitForText("Tambah pekerja")

        composeRule.onNodeWithText(financeLabel).performClick()
        if (BuildConfig.BUSINESS_TYPE != "RETAIL") {
            waitForText("Shift kasir")
        }
        composeRule.onAllNodesWithText("Utang & Piutang")[0].performClick()
        waitForText("Tambah utang")
        if (BuildConfig.BUSINESS_TYPE != "CULINARY") {
            waitForText("Tambah piutang")
            waitForText("Tambah pelanggan")
        }
        composeRule.onAllNodesWithText("Transaksi")[0].performClick()
        composeRule.onAllNodesWithText("Kas")[0].performClick()
        waitForText("Shift kasir")

        composeRule.onNodeWithText("Laporan").performClick()
        waitForText("Omzet hari ini")
        waitForText("Perkiraan penjualan")
        waitForText("Ganti PIN")

        composeRule.onNodeWithText("Lainnya").performClick()
        waitForText("Profil usaha")
        waitForText("Ubah nama usaha")
        waitForText("Backup & restore")
        waitForText("Buat backup lokal")
        waitForText("Pilih file untuk restore")
        waitForText("Keluar Mode Owner")
        composeRule.onNodeWithText("Offline-first", substring = true).performScrollTo()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("owner-exit").assertIsDisplayed().performClick()
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

    private fun waitForText(text: String, substring: Boolean = false) {
        try {
            composeRule.waitUntil(timeoutMillis = 5_000) {
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
}
