package com.bimacore.usahakecil.data

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DatabaseOperationCoordinator {
    private val mutex = Mutex()

    suspend fun <T> withOperation(action: suspend () -> T): T {
        val active = coroutineContext[OperationContext]
        if (active?.coordinator === this) return action()
        return mutex.withLock {
            withContext(OperationContext(this)) { action() }
        }
    }

    private class OperationContext(
        val coordinator: DatabaseOperationCoordinator,
    ) : AbstractCoroutineContextElement(OperationContext) {
        companion object : CoroutineContext.Key<OperationContext>
    }
}
