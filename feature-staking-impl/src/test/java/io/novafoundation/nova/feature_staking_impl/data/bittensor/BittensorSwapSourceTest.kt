package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BittensorSwapSourceTest {

    private val calls = SwapCalls()
    private val runtimeCalls = mock(MultiChainRuntimeCallsApi::class.java)
    private val rpc = mock(RpcCalls::class.java)
    private val source = RealBittensorSwapSource(runtimeCalls, rpc)
    private val subnet = BittensorNetuid.Subnet(64)

    @Test
    fun `buy and sell pin both requests even when head changes between responses`() = runBlocking<Unit> {
        `when`(runtimeCalls.forChain("bittensor")).thenReturn(calls)
        `when`(rpc.getBlockHash("bittensor")).thenAnswer { calls.head }

        for (direction in BittensorSwapDirection.entries) {
            val initialHead = calls.head
            calls.blocks.clear()
            calls.methods.clear()
            val quote = source.quote("bittensor", subnet, direction, BigInteger.valueOf(100))

            assertEquals(initialHead, quote.blockHash)
            assertEquals(listOf(initialHead, initialHead), calls.blocks)
            assertTrue(calls.head != initialHead)
            assertEquals(BigInteger.valueOf(201), quote.spotPrice)
            assertEquals(direction, quote.simulation.direction)
            val simulationMethod = if (direction == BittensorSwapDirection.TAO_TO_ALPHA) "sim_swap_tao_for_alpha" else "sim_swap_alpha_for_tao"
            assertEquals(setOf(simulationMethod, "current_alpha_price"), calls.methods.toSet())
        }
        verify(rpc, times(2)).getBlockHash("bittensor")
        verify(runtimeCalls, times(2)).forChain("bittensor")
    }

    @Test
    fun `pinned request failure and cancellation do not fall back to latest`() = runBlocking<Unit> {
        `when`(runtimeCalls.forChain("bittensor")).thenReturn(calls)
        `when`(rpc.getBlockHash("bittensor")).thenReturn("0xinitial")
        for (failure in listOf(IllegalStateException("Block unavailable"), CancellationException("Quote replaced"))) {
            calls.failure = failure
            val result = runCatching { source.quote("bittensor", subnet, BittensorSwapDirection.TAO_TO_ALPHA, BigInteger.valueOf(100)) }
            val actualFailure = requireNotNull(result.exceptionOrNull())
            assertEquals(failure.javaClass, actualFailure.javaClass)
            assertEquals(failure.message, actualFailure.message)
        }
    }

    private class SwapCalls : RuntimeCallsApi {
        override val chainId = "bittensor"
        override val runtime = mock(RuntimeSnapshot::class.java)
        var head = "0xinitial"
        var failure: Exception? = null
        val blocks = mutableListOf<String>()
        val methods = mutableListOf<String>()

        override fun isSupported(section: String, method: String) = true

        override suspend fun <R> callAt(
            blockHash: String,
            section: String,
            method: String,
            arguments: Map<String, Any?>,
            returnBinding: (Any?) -> R
        ): R {
            failure?.let { throw it }
            assertEquals("SwapRuntimeApi", section)
            assertEquals(BigInteger.valueOf(64), arguments["netuid"])
            blocks += blockHash
            methods += method
            head += "-next"
            yield()
            if (method == "current_alpha_price") return returnBinding(BigInteger.valueOf(201))

            val inputKey = if (method == "sim_swap_tao_for_alpha") "tao" else "alpha"
            assertEquals(BigInteger.valueOf(100), arguments[inputKey])
            val decoded = Struct.Instance(
                mapOf(
                    "taoAmount" to BigInteger.valueOf(100),
                    "alphaAmount" to BigInteger.valueOf(100),
                    "taoFee" to BigInteger.ZERO,
                    "alphaFee" to BigInteger.ZERO
                )
            )
            return returnBinding(decoded)
        }

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: Map<String, Any?>,
            returnBinding: (Any?) -> R
        ): R = error("Quote must not use latest state")

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: List<Pair<Any?, String?>>,
            returnType: String,
            returnBinding: (Any?) -> R
        ): R = error("Quote must use metadata encoding")
    }
}
