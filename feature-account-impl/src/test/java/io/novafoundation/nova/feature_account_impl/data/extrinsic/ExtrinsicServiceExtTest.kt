package io.novafoundation.nova.feature_account_impl.data.extrinsic

import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitStatus
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicWatchException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock

class ExtrinsicServiceExtTest {

    private val account = mock(MetaAccount::class.java)

    private val hierarchy = SubmissionHierarchy(account, CallExecutionType.IMMEDIATE)

    private val extrinsicHash = "0xoriginal"

    @Test
    fun `terminal failures stop inclusion waiting even if upstream stays open`() = runBlocking {
        val failures = listOf(
            ExtrinsicStatus.Invalid(extrinsicHash),
            ExtrinsicStatus.Dropped(extrinsicHash),
            ExtrinsicStatus.Usurped(replacementHash = "0xreplacement", extrinsicHash),
            ExtrinsicStatus.FinalityTimeout(blockHash = "0xblock", extrinsicHash)
        )

        for (failure in failures) {
            var released = false
            val updates = flow {
                try {
                    emit(watch(ExtrinsicStatus.Ready(extrinsicHash)))
                    emit(watch(failure))
                    awaitCancellation()
                } finally {
                    released = true
                }
            }

            val result = withTimeout(timeMillis = 1_000) { Result.success(updates).awaitInBlock() }
            val error = result.exceptionOrNull() as ExtrinsicWatchException
            assertSame(failure, error.status)
            assertEquals(extrinsicHash, error.status.extrinsicHash)
            assertTrue(released)
        }
    }

    @Test
    fun `inclusion returns original status and hierarchy and releases subscription`() = runBlocking {
        val included = ExtrinsicStatus.InBlock(blockHash = "0xblock", extrinsicHash)
        var released = false
        val updates = flow {
            try {
                emit(watch(ExtrinsicStatus.Ready(extrinsicHash)))
                emit(watch(ExtrinsicStatus.Broadcast(extrinsicHash)))
                emit(watch(ExtrinsicStatus.Other(extrinsicHash)))
                emit(watch(included))
                fail("Must stop collecting after inclusion")
            } finally {
                released = true
            }
        }

        val result = Result.success(updates).awaitInBlock().getOrThrow()

        assertSame(included, result.status)
        assertSame(hierarchy, result.submissionHierarchy)
        assertTrue(released)
    }

    @Test
    fun `finality timeout after inclusion cannot satisfy finalized waiter`() = runBlocking {
        val timeout = ExtrinsicStatus.FinalityTimeout(blockHash = "0xblock", extrinsicHash)
        val updates = flowOf(watch(ExtrinsicStatus.InBlock(blockHash = "0xblock", extrinsicHash)), watch(timeout))

        try {
            updates.awaitStatus<ExtrinsicStatus.Finalized>()
            fail("Timeout must not be treated as finalized")
        } catch (error: ExtrinsicWatchException) {
            assertSame(timeout, error.status)
        }
    }

    @Test
    fun `actual finalized status still satisfies finalized waiter`() = runBlocking {
        val finalized = ExtrinsicStatus.Finalized(blockHash = "0xblock", extrinsicHash)
        val updates = flowOf(watch(ExtrinsicStatus.InBlock(blockHash = "0xblock", extrinsicHash)), watch(finalized))

        val result = updates.awaitStatus<ExtrinsicStatus.Finalized>()

        assertSame(finalized, result.status)
        assertSame(hierarchy, result.submissionHierarchy)
    }

    @Test
    fun `unmatched terminal status ends wait without inventing inclusion`() = runBlocking {
        val finalized = ExtrinsicStatus.Finalized(blockHash = "0xblock", extrinsicHash)
        val updates = flowOf(watch(finalized))

        val result = Result.success(updates).awaitInBlock()

        assertSame(finalized, (result.exceptionOrNull() as ExtrinsicWatchException).status)
    }

    @Test
    fun `submission and upstream failures retain their causes`() = runBlocking {
        val failure = IllegalStateException("RPC failed")
        val submission = Result.failure<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>>(failure)
        val updates = flow<ExtrinsicWatchResult<ExtrinsicStatus>> { throw failure }

        assertSame(failure, submission.awaitInBlock().exceptionOrNull())
        assertSame(failure, Result.success(updates).awaitInBlock().exceptionOrNull())
    }

    @Test
    fun `caller cancellation propagates and releases subscription`() = runBlocking {
        val cancellation = CancellationException("Caller cancelled")
        var released = false
        val updates = flow {
            try {
                emit(watch(ExtrinsicStatus.Ready(extrinsicHash)))
                throw cancellation
            } finally {
                released = true
            }
        }

        try {
            Result.success(updates).awaitInBlock()
            fail("Cancellation must propagate")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
        assertTrue(released)
    }

    @Test
    fun `cancellation returned from preparation is rethrown`() = runBlocking {
        val cancellation = CancellationException("Preparation cancelled")
        val submission = Result.failure<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>>(cancellation)

        try {
            submission.awaitInBlock()
            fail("Cancellation must propagate")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }

    @Test
    fun `watch completion without inclusion is not success`() = runBlocking {
        val updates = flowOf(watch(ExtrinsicStatus.Ready(extrinsicHash)))

        assertTrue(Result.success(updates).awaitInBlock().isFailure)
    }

    private fun watch(status: ExtrinsicStatus): ExtrinsicWatchResult<ExtrinsicStatus> {
        return ExtrinsicWatchResult(status, hierarchy)
    }
}
