package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
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
            controller.observe("asset") {
                flow {
                    independentLoads++
                    emit(7)
                    awaitCancellation()
                }
            }.filterLoaded().collect {}
        }
        val retriable = launch {
            controller.observe("positions") {
                flow {
                    failedLoads++
                    check(failedLoads > 1) { "RPC unavailable" }
                    emit(42)
                }
            }.filterLoaded().take(1).toList(loadedValues)
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
}
