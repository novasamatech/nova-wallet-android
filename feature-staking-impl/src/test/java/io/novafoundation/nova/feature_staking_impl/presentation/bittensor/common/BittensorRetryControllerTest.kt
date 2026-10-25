package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
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
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novafoundation.nova.infrastructure.attestation.AttestationRefusedException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnavailableException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnsupportedException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import retrofit2.HttpException
import retrofit2.Response

class BittensorRetryControllerTest {

    private val resources = mock(ResourceManager::class.java)

    private val logger = mock(Logger::class.java)

    private val controller = BittensorRetryController(resources, logger, mock(AutomaticInteractionGate::class.java))

    @Before
    fun prepare() {
        `when`(resources.getString(R.string.common_error_general_title)).thenReturn("Error")
        `when`(resources.getString(R.string.bittensor_load_failed_message)).thenReturn("Retry")
        `when`(resources.getString(R.string.bittensor_load_rejected_message)).thenReturn("Rejected message")
        `when`(resources.getString(R.string.bittensor_load_slow_title)).thenReturn("Slow")
        `when`(resources.getString(R.string.bittensor_load_slow_message)).thenReturn("Slow message")
        `when`(resources.getString(R.string.bittensor_load_device_title)).thenReturn("Device")
        `when`(resources.getString(R.string.bittensor_load_device_message)).thenReturn("Device message")
    }

    @Test
    fun `generic failure uses the iOS load failure wording and offers retry`() = runBlocking {
        val failure = firstFailure { error("RPC unavailable") }

        assertEquals(BittensorReadFailureKind.GENERIC, failure.kind)
        assertEquals("Error", failure.title)
        assertEquals("Retry", failure.message)
        assertTrue(failure.retryable)
    }

    @Test
    fun `timeout is explained as a slow network and stays retryable`() = runBlocking {
        val failure = firstFailure { throw BittensorTimeoutException("Subnet catalogue") }

        assertEquals(BittensorReadFailureKind.SLOW_NETWORK, failure.kind)
        assertEquals("Slow", failure.title)
        assertEquals("Slow message", failure.message)
        assertTrue(failure.retryable)
    }

    @Test
    fun `device without attestation gets one non retryable explanation per screen`() = runBlocking {
        val unsupported = IOException("Profile-3 authentication failed", AttestationUnsupportedException("Play Integrity is unavailable"))
        val failure = firstFailure { throw unsupported }

        assertEquals(BittensorReadFailureKind.DEVICE_UNSUPPORTED, failure.kind)
        assertEquals("Device", failure.title)
        assertEquals("Device message", failure.message)
        assertFalse(failure.retryable)

        failure.onDismiss()
        val laterAttempts = AtomicInteger()
        val later = launch {
            controller.observe("later source", transform = { flow -> flow.catch { } }) {
                flow<Int> {
                    laterAttempts.incrementAndGet()
                    throw unsupported
                }
            }.collect {}
        }
        withTimeout(3_000L) { while (laterAttempts.get() == 0) yield() }

        assertNull(withTimeoutOrNull(100L) { controller.events.first() })
        later.cancelAndJoin()
    }

    @Test
    fun `failures raised while a dialog is pending share it and its retry restarts every failed source`() = runBlocking {
        val attempts = List(2) { AtomicInteger() }
        val values = mutableListOf<Int>()
        val collections = attempts.mapIndexed { index, sourceAttempts ->
            launch {
                controller.observe("source $index", transform = { it.catch { } }) {
                    flow {
                        check(sourceAttempts.incrementAndGet() > 1) { "RPC unavailable" }
                        emit(index)
                    }
                }.take(1).toList(values)
            }
        }

        withTimeout(3_000L) {
            while (attempts.any { it.get() == 0 }) yield()
            controller.events.first().onRetry()
            collections.joinAll()
        }

        assertEquals(setOf(0, 1), values.toSet())
        assertEquals(listOf(2, 2), attempts.map { it.get() })
        assertNull(withTimeoutOrNull(100L) { controller.events.first() })
    }

    @Test
    fun `temporary failures behind an unsupported device dialog remain retryable`() = runBlocking {
        val unsupported = AttestationUnsupportedException("Play Integrity is unavailable")
        val deviceFailure = firstFailure { throw unsupported }
        val attempts = List(2) { AtomicInteger() }
        val reported = List(2) { CompletableDeferred<Unit>() }
        val values = mutableListOf<Int>()
        val collections = attempts.mapIndexed { index, sourceAttempts ->
            launch {
                controller.observe(
                    step = "RPC $index",
                    transform = { source -> source.catch { reported[index].complete(Unit) } }
                ) {
                    flow {
                        if (sourceAttempts.incrementAndGet() == 1) throw SocketTimeoutException()
                        emit(index)
                    }
                }.take(1).toList(values)
            }
        }
        withTimeout(3_000L) { reported.forEach { it.await() } }
        assertSame(deviceFailure, controller.events.first())

        deviceFailure.onDismiss()
        val retryFailure = withTimeout(3_000L) { controller.events.first() }
        assertEquals(BittensorReadFailureKind.SLOW_NETWORK, retryFailure.kind)
        assertTrue(retryFailure.retryable)
        assertEquals(listOf(1, 1), attempts.map { it.get() })
        // A late dismissal from the old dialog must not clear the newly pending retry.
        deviceFailure.onDismiss()
        assertSame(retryFailure, controller.events.first())

        retryFailure.onRetry()
        withTimeout(3_000L) { collections.joinAll() }
        assertEquals(setOf(0, 1), values.toSet())
        assertEquals(listOf(2, 2), attempts.map { it.get() })
        assertNull(withTimeoutOrNull(100L) { controller.events.first() })
    }

    @Test
    fun `queued retry can be skipped without reloading the failed source`() = runBlocking {
        val deviceFailure = firstFailure { throw AttestationUnsupportedException("Unsupported") }
        val reported = CompletableDeferred<Unit>()
        var attempts = 0
        val collection = launch {
            controller.observe(
                step = "RPC",
                transform = { source -> source.catch { reported.complete(Unit) } }
            ) {
                flow<Int> {
                    attempts++
                    throw SocketTimeoutException()
                }
            }.collect {}
        }
        withTimeout(3_000L) { reported.await() }
        deviceFailure.onDismiss()
        val retryFailure = withTimeout(3_000L) { controller.events.first() }
        assertTrue(retryFailure.retryable)

        retryFailure.onDismiss()
        assertNull(withTimeoutOrNull(100L) { controller.events.first() })
        assertEquals(1, attempts)
        collection.cancelAndJoin()
    }

    @Test
    fun `pending failure is shown again to a recreated screen until it is dismissed`() = runBlocking {
        val failure = firstFailure { error("RPC unavailable") }

        assertSame(failure, withTimeout(3_000L) { controller.events.first() })

        failure.onDismiss()

        assertNull(withTimeoutOrNull(100L) { controller.events.first() })
    }

    @Test
    fun `failure kind follows the cause chain`() {
        assertEquals(BittensorReadFailureKind.SLOW_NETWORK, IllegalStateException("wrapped", SocketTimeoutException()).bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.DEVICE_UNSUPPORTED, AttestationUnsupportedException("refused").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, AttestationUnavailableException("cooling down").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, IllegalArgumentException("Subnet metadata is unavailable").bittensorReadFailureKind())
    }

    @Test
    fun `backend rejection is explained without retry`() = runBlocking {
        val failure = firstFailure { throw httpFailure(422) }

        assertEquals(BittensorReadFailureKind.REJECTED, failure.kind)
        assertEquals("Error", failure.title)
        assertEquals("Rejected message", failure.message)
        assertFalse(failure.retryable)
    }

    @Test
    fun `backend error statuses follow the iOS grading`() {
        assertEquals(BittensorReadFailureKind.REJECTED, httpFailure(400).bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.REJECTED, httpFailure(404, "route_not_found").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.REJECTED, httpFailure(409, "client_already_registered").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, httpFailure(404).bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, httpFailure(403, "forbidden").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, httpFailure(429).bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, httpFailure(503).bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, AttestationUnavailableException("Bootstrap failed", httpFailure(422)).bittensorReadFailureKind())
    }

    @Test
    fun `attestation refusals follow the iOS grading`() {
        assertEquals(BittensorReadFailureKind.DEVICE_UNSUPPORTED, refusal(401, "attestation_failed").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.DEVICE_UNSUPPORTED, refusal(403, "app_not_allowed").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.DEVICE_UNSUPPORTED, refusal(403, "binding_not_allowed").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.DEVICE_UNSUPPORTED, bearerDenial().bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.REJECTED, refusal(409, "client_already_registered").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.REJECTED, refusal(403, "insufficient_scope").bittensorReadFailureKind())
        assertEquals(BittensorReadFailureKind.GENERIC, httpFailure(401, "invalid_token").bittensorReadFailureKind())
    }

    private fun refusal(status: Int, code: String) =
        IOException("Profile-3 authentication failed", AttestationRefusedException(status, code, "Profile-3 registration was refused"))

    private fun bearerDenial(): HttpException {
        val raw = okhttp3.Response.Builder()
            .request(okhttp3.Request.Builder().url("https://infra.test/v1/bittensor/subnets").build())
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .header("WWW-Authenticate", "Bearer error=\"invalid_token\"")
            .build()
        val body = "{\"error\":{\"code\":\"invalid_token\"}}".toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Unit>(body, raw))
    }

    private fun httpFailure(status: Int, code: String? = null): HttpException {
        val body = code?.let { "{\"error\":{\"code\":\"$it\"}}" }.orEmpty()
        return HttpException(Response.error<Unit>(status, body.toResponseBody("application/json".toMediaType())))
    }

    private suspend fun firstFailure(load: suspend () -> Int): BittensorReadFailure = coroutineScope {
        val collection = launch {
            controller.observe("step", transform = { it.catch { } }) { flow { emit(load()) } }.collect {}
        }
        val failure = withTimeout(3_000L) { controller.events.first() }
        collection.cancelAndJoin()
        failure
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
    fun `request timeout exposes error and dialog retry reloads the source`() = runBlocking {
        var attempts = 0
        val states = mutableListOf<ExtendedLoadingState<Int>>()
        val collection = launch {
            controller.observe("chart", transform = { it.withSafeLoading() }) {
                flow {
                    attempts++
                    if (attempts == 1) {
                        withTimeout(20L) { awaitCancellation() }
                    }
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

        assertEquals(2, attempts)
        assertEquals(ExtendedLoadingState.Loading, states[0])
        assertTrue(states[1] is ExtendedLoadingState.Error)
        assertEquals(ExtendedLoadingState.Loading, states[2])
        assertEquals(ExtendedLoadingState.Loaded(42), states[3])
    }

    @Test
    fun `collector cancellation during timed request does not produce a dialog`() = runBlocking {
        val sourceStarted = CompletableDeferred<Unit>()
        val collection = launch {
            controller.observe("chart", transform = { it.withSafeLoading() }) {
                flow<Int> {
                    withTimeout(3_000L) {
                        sourceStarted.complete(Unit)
                        awaitCancellation()
                    }
                }
            }.collect {}
        }

        withTimeout(3_000L) { sourceStarted.await() }
        collection.cancelAndJoin()

        assertNull(withTimeoutOrNull(50L) { controller.events.first() })
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
