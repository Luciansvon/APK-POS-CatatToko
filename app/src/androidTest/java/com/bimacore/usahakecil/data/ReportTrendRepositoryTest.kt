package com.bimacore.usahakecil.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportTrendRepositoryTest {
    private lateinit var database: PosDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            PosDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun empty_trend_keeps_all_zero_buckets_for_monitoring() = runBlocking {
        val repository = ReportRepository(database, ReportSession().also { it.unlock() })

        val trend = repository.readTrend(
            ReportChartGranularity.DAILY,
            now = 1_754_147_100_000L,
        )

        assertEquals(14, trend.points.size)
        assertTrue(trend.points.all { point ->
            point.sales == 0L &&
                point.transactionCount == 0 &&
                point.quantity == 0L &&
                point.cashIn == 0L &&
                point.cashOut == 0L &&
                point.netCash == 0L
        })
        assertTrue(trend.products.isEmpty())
    }
}
