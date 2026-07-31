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
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun catalog_and_calculator_are_accessible() {
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
        composeRule.onNodeWithTag("start-transaction").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Keripik Singkong")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("product-101").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("continue-payment").assertIsEnabled()
                true
            }.getOrDefault(false)
        }
        if (composeRule.onAllNodesWithTag("cart-summary").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("cart-summary").performClick()
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

        composeRule.onNodeWithText("Lainnya").performClick()
        composeRule.onNodeWithText("Backup & restore").assertIsDisplayed()
        composeRule.onNodeWithText("Buat backup lokal").assertIsDisplayed()
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
}
