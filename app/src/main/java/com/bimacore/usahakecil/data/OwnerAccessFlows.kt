package com.bimacore.usahakecil.data

import com.bimacore.usahakecil.security.ReportSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal fun <T> Flow<T>.ownerOnly(
    session: ReportSession,
    lockedValue: T,
): Flow<T> = combine(session.unlocked) { value, unlocked ->
    if (unlocked) value else lockedValue
}
