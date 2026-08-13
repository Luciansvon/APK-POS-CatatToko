package com.bimacore.usahakecil.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DatabaseOperationCoordinatorTest {
    @Test
    fun operations_wait_for_the_active_database_operation() = runTest {
        val coordinator = DatabaseOperationCoordinator()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondEntered = CompletableDeferred<Unit>()

        val first = launch {
            coordinator.withOperation {
                firstEntered.complete(Unit)
                releaseFirst.await()
            }
        }
        firstEntered.await()
        val second = async {
            coordinator.withOperation { secondEntered.complete(Unit) }
        }
        runCurrent()

        assertFalse(secondEntered.isCompleted)
        releaseFirst.complete(Unit)
        second.await()
        first.join()
        assertTrue(secondEntered.isCompleted)
    }

    @Test
    fun nested_operation_on_same_coordinator_does_not_deadlock() = runTest {
        val coordinator = DatabaseOperationCoordinator()
        var completed = false

        coordinator.withOperation {
            coordinator.withOperation { completed = true }
        }

        assertTrue(completed)
    }
}
