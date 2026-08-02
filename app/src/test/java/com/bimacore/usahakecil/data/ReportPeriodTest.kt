package com.bimacore.usahakecil.data

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportPeriodTest {
    @Test
    fun previous_period_is_before_current_period_for_all_options() {
        val now = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        ReportPeriod.values().forEach { period ->
            val current = period.range(now)
            val previous = period.previousRange(now)
            assertTrue(previous.first < previous.last)
            assertTrue(previous.first < current.first)
            assertTrue(previous.last < current.last)
        }
    }

    @Test
    fun month_and_year_previous_ranges_keep_calendar_boundaries() {
        val now = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val previousMonthStart = Calendar.getInstance().apply {
            timeInMillis = ReportPeriod.MONTH.previousRange(now).first
        }
        assertEquals(Calendar.JULY, previousMonthStart.get(Calendar.MONTH))
        assertEquals(1, previousMonthStart.get(Calendar.DAY_OF_MONTH))

        val previousYearStart = Calendar.getInstance().apply {
            timeInMillis = ReportPeriod.YEAR.previousRange(now).first
        }
        assertEquals(2025, previousYearStart.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, previousYearStart.get(Calendar.MONTH))
        assertEquals(1, previousYearStart.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun previous_ranges_compare_the_same_elapsed_time() {
        val now = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 2)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 45)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val expectedPreviousEnds = mapOf(
            ReportPeriod.DAY to Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, -1)
            }.timeInMillis,
            ReportPeriod.WEEK to Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, -7)
            }.timeInMillis,
            ReportPeriod.MONTH to Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.MONTH, -1)
            }.timeInMillis,
            ReportPeriod.YEAR to Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.YEAR, -1)
            }.timeInMillis,
        )

        ReportPeriod.values().forEach { period ->
            assertEquals(expectedPreviousEnds.getValue(period), period.previousRange(now).last)
        }
    }
}
