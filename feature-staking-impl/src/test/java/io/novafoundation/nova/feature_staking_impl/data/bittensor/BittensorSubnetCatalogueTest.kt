package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BittensorSubnetCatalogueTest {

    private val calls = CatalogueCalls()

    private val runtimeCalls = object : MultiChainRuntimeCallsApi {
        override suspend fun forChain(chainId: String): RuntimeCallsApi = calls

        override suspend fun isSupported(chainId: String, section: String, method: String) = true
    }

    private val config = BittensorConfigRepository { fallbackBittensorConfig() }

    private val swaps = mock(BittensorSwapSource::class.java)

    private val repository = RealBittensorSubnetRepository(runtimeCalls, config, swaps)

    @Test
    fun `reuses catalogue metadata but fetches a current price for individual subnet lookups`() = runBlocking<Unit> {
        val subnetId = BittensorSubnetId(1)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.TEN, BigInteger.valueOf(20))

        repository.subnets()
        repository.subnets()
        assertEquals(BigInteger.TEN, repository.subnet(subnetId).priceInRao)
        assertEquals(BigInteger.valueOf(20), repository.subnet(subnetId).priceInRao)

        assertEquals(1, calls.requests)
        verify(swaps, times(2)).currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)
    }

    private class CatalogueCalls : RuntimeCallsApi {

        override val chainId = ChainGeneses.BITTENSOR

        override val runtime = mock(RuntimeSnapshot::class.java)

        var requests = 0

        override fun isSupported(section: String, method: String) = true

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: Map<String, Any?>,
            returnBinding: (Any?) -> R
        ): R {
            requests++
            val subnet = Struct.Instance(
                mapOf(
                    "netuid" to BigInteger.ONE,
                    "subnetName" to "Name".encodeToByteArray(),
                    "tokenSymbol" to "α".encodeToByteArray(),
                    "taoIn" to BigInteger.valueOf(1000),
                    "alphaIn" to BigInteger.valueOf(1000)
                )
            )
            return returnBinding(listOf(subnet))
        }

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: List<Pair<Any?, String?>>,
            returnType: String,
            returnBinding: (Any?) -> R
        ): R = error("Unexpected untyped runtime request")
    }
}
