package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_staking_impl.R
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorRetryControllerTest {

    private val resources = mock(ResourceManager::class.java)

    private val logger = mock(Logger::class.java)

    private val controller = BittensorRetryController(resources, logger)

    @Before
    fun prepare() {
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(resources.getString(R.string.common_retry_message)).thenReturn("Retry")
    }

    @Test
    fun `dialog retry restarts a failed source without reloading an independent source`() = runBlocking {
        var failedLoads = 0
        var independentLoads = 0
        val loadedValues = mutableListOf<Int>()
        val independent = launch(start = CoroutineStart.UNDISPATCHED) {
            controller.observe("asset", transform = { it.catch { } }) {
                flow {
                    independentLoads++
                    emit(7)
                    awaitCancellation()
                }
            }.collect {}
        }
        val retriable = launch {
            controller.observe("positions", transform = { it.catch { } }) {
                flow {
                    failedLoads++
                    check(failedLoads > 1) { "RPC unavailable" }
                    emit(42)
                }
            }.take(1).toList(loadedValues)
        }

        withTimeout(3_000L) {
            controller.events.first().onRetry()
            retriable.join()
        }

        assertEquals(listOf(42), loadedValues)
        assertEquals(2, failedLoads)
        assertEquals(1, independentLoads)
        independent.cancelAndJoin()
    }

    @Test
    fun `UI loading transformation resets on each retry and exposes the error`() = runBlocking {
        var attempts = 0
        val states = mutableListOf<ExtendedLoadingState<Int>>()
        val collection = launch {
            controller.observe("positions", transform = { it.withSafeLoading() }) {
                flow {
                    attempts++
                    check(attempts > 1) { "Unavailable" }
                    emit(42)
                }
            }
                .take(4)
                .toList(states)
        }

        withTimeout(3_000L) {
            controller.events.first().onRetry()
            collection.join()
        }

        assertEquals(ExtendedLoadingState.Loading, states[0])
        assertTrue(states[1] is ExtendedLoadingState.Error)
        assertEquals(ExtendedLoadingState.Loading, states[2])
        assertEquals(ExtendedLoadingState.Loaded(42), states[3])
    }

    @Test
    fun `failure while creating the source can be retried too`() = runBlocking {
        var attempts = 0
        val values = mutableListOf<Int>()
        val collection = launch {
            controller.observe("positions", transform = { it.catch { } }) {
                attempts++
                check(attempts > 1) { "Failed before returning a flow" }
                flowOf(42)
            }
                .take(1)
                .toList(values)
        }

        withTimeout(3_000L) {
            controller.events.first().onRetry()
            collection.join()
        }

        assertEquals(listOf(42), values)
        assertEquals(2, attempts)
    }

    @Test
    fun `cancellation does not produce an error dialog`() = runBlocking {
        val sourceStarted = CompletableDeferred<Unit>()
        val collection = launch {
            controller.observe("positions", transform = { it }) {
                flow<Int> {
                    sourceStarted.complete(Unit)
                    throw CancellationException("Cancelled")
                }
            }.collect {}
        }

        withTimeout(3_000L) { sourceStarted.await() }

        assertNull(withTimeoutOrNull(50L) { controller.events.first() })
        collection.cancelAndJoin()
    }
}
