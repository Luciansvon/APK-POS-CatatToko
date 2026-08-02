package com.bimacore.usahakecil.data

import java.util.Calendar

enum class ReportPeriod(
    val label: String,
) {
    DAY("Hari ini"),
    WEEK("Minggu ini"),
    MONTH("Bulan ini"),
    YEAR("Tahun ini"),
    ;

    fun range(now: Long = System.currentTimeMillis()): LongRange {
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            when (this@ReportPeriod) {
                DAY -> Unit
                WEEK -> {
                    val daysFromMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
                    add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                }
                MONTH -> set(Calendar.DAY_OF_MONTH, 1)
                YEAR -> {
                    set(Calendar.MONTH, Calendar.JANUARY)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
            }
        }.timeInMillis
        return start..now
    }

    fun previousRange(now: Long = System.currentTimeMillis()): LongRange {
        val currentStart = range(now).first
        val previousStart = Calendar.getInstance().apply {
            timeInMillis = currentStart
            when (this@ReportPeriod) {
                DAY -> add(Calendar.DAY_OF_MONTH, -1)
                WEEK -> add(Calendar.DAY_OF_MONTH, -7)
                MONTH -> add(Calendar.MONTH, -1)
                YEAR -> add(Calendar.YEAR, -1)
            }
        }.timeInMillis
        val previousEnd = Calendar.getInstance().apply {
            timeInMillis = now
            when (this@ReportPeriod) {
                DAY -> add(Calendar.DAY_OF_MONTH, -1)
                WEEK -> add(Calendar.DAY_OF_MONTH, -7)
                MONTH -> add(Calendar.MONTH, -1)
                YEAR -> add(Calendar.YEAR, -1)
            }
        }.timeInMillis
        return previousStart..previousEnd
    }
}
