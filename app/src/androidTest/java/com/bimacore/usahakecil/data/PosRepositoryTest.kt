package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.domain.AddToCartResult
import com.bimacore.usahakecil.domain.BusinessType
import com.bimacore.usahakecil.domain.CheckoutResult
import com.bimacore.usahakecil.domain.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PosRepositoryTest {
    private lateinit var database: PosDatabase
    private lateinit var repository: PosRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PosRepository(
            database = database,
            businessType = BusinessType.RETAIL,
            businessName = "Retail Test",
            clock = { 1_700_000_000_000L },
        )
        runBlocking {
            database.shiftDao().insertShift(
                ShiftEntity(
                    cashierName = "Kasir Test",
                    openedAt = 0,
                    openingCash = 0,
                    openSlot = 1,
                ),
            )
        }
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun sale_is_atomic_and_duplicate_submit_returns_same_receipt() = runTest {
        repository.seedIfNeeded()
        assertEquals(AddToCartResult.Added, repository.addProduct(101))

        val first = repository.completeSale(
            CheckoutRequest(
                method = PaymentMethod.CASH,
                amountReceived = 20_000,
                externalPaymentConfirmed = false,
            ),
        )
        assertTrue(first is CheckoutResult.Success)
        val firstReceipt = (first as CheckoutResult.Success).receipt
        assertEquals(12_000L, firstReceipt.total)
        assertEquals(8_000L, firstReceipt.changeAmount)
        assertEquals(
            database.shiftDao().getOpenShift()?.id,
            database.saleDao().getSale(firstReceipt.saleId)?.shiftId,
        )
        val movements = database.stockDao().getMovementsForSale(firstReceipt.saleId)
        assertEquals(1, movements.size)
        assertEquals(-1, movements.single().quantityDelta)
        assertEquals("SALE", movements.single().type)

        val duplicate = repository.completeSale(
            CheckoutRequest(
                method = PaymentMethod.CASH,
                amountReceived = 20_000,
                externalPaymentConfirmed = false,
            ),
        ) as CheckoutResult.Success
        assertEquals(firstReceipt.saleId, duplicate.receipt.saleId)

        repository.newTransaction()
        assertTrue(repository.snapshot.first().cartItems.isEmpty())
    }

    @Test
    fun sale_requires_an_open_shift() = runTest {
        repository.seedIfNeeded()
        val openShift = requireNotNull(database.shiftDao().getOpenShift())
        database.shiftDao().updateShift(
            openShift.copy(
                status = ShiftStatus.CLOSED.name,
                closedAt = 1_700_000_000_001L,
                closingCash = 0,
                openSlot = null,
            ),
        )
        assertEquals(AddToCartResult.Added, repository.addProduct(101))

        val result = repository.completeSale(
            CheckoutRequest(PaymentMethod.CASH, 20_000, false),
        )

        assertTrue(result is CheckoutResult.Error)
        assertTrue((result as CheckoutResult.Error).message.contains("Buka shift"))
    }
}
