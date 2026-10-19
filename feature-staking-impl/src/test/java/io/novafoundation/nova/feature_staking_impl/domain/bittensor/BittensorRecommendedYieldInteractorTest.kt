package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorAlphaYieldRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRecommendationsRemote
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorValidatorBackend
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class BittensorRecommendedYieldInteractorTest {

    private val backend = mock(BittensorValidatorBackend::class.java)

    private val yields = mock(BittensorAlphaYieldRepository::class.java)

    private val chainRegistry = mock(ChainRegistry::class.java)

    private val logger = mock(Logger::class.java)

    private val chain = mock(Chain::class.java)

    private val interactor = BittensorRecommendedYieldInteractor(backend, yields, chainRegistry, logger)

    private val firstSubnet = BittensorNetuid.Subnet(1)

    private val secondSubnet = BittensorNetuid.Subnet(2)

    private val firstHotkey = AccountIdKey(ByteArray(32) { 1 })

    private val secondHotkey = AccountIdKey(ByteArray(32) { 2 })

    @Before
    fun prepare() = runBlocking<Unit> {
        `when`(chainRegistry.getChain(ChainGeneses.BITTENSOR)).thenReturn(chain)
        val recommendations = listOf(recommendation(firstSubnet, firstHotkey), recommendation(secondSubnet, secondHotkey))
        `when`(backend.recommendedValidators()).thenReturn(recommendations)
    }

    @Test
    fun `only recommended validators contribute to maximum yield`() = runBlocking<Unit> {
        val firstRates = mapOf(firstHotkey to BigDecimal.ONE, secondHotkey to BigDecimal.TEN)
        `when`(yields.alphaYields(chain, firstSubnet)).thenReturn(firstRates)
        `when`(yields.alphaYields(chain, secondSubnet)).thenReturn(emptyMap())

        assertEquals(mapOf(firstSubnet to BigDecimal.ONE), interactor.maximumYields())
    }

    @Test
    fun `partial failure preserves successful rates including zero`() = runBlocking<Unit> {
        `when`(yields.alphaYields(chain, firstSubnet)).thenThrow(IllegalStateException("Unavailable"))
        `when`(yields.alphaYields(chain, secondSubnet)).thenReturn(mapOf(secondHotkey to BigDecimal.ZERO))

        assertEquals(mapOf(secondSubnet to BigDecimal.ZERO), interactor.maximumYields())
    }

    @Test(expected = IllegalStateException::class)
    fun `failure without usable rates is reported`() = runBlocking<Unit> {
        `when`(yields.alphaYields(chain, firstSubnet)).thenThrow(IllegalStateException("Unavailable"))
        `when`(yields.alphaYields(chain, secondSubnet)).thenReturn(emptyMap())

        interactor.maximumYields()
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is not a partial failure`() = runBlocking<Unit> {
        `when`(yields.alphaYields(chain, firstSubnet)).thenThrow(CancellationException())
        `when`(yields.alphaYields(chain, secondSubnet)).thenReturn(mapOf(secondHotkey to BigDecimal.ONE))

        interactor.maximumYields()
    }

    private fun recommendation(subnetId: BittensorNetuid.Subnet, hotkey: AccountIdKey): BittensorRecommendationsRemote.Item {
        return BittensorRecommendationsRemote.Item(subnetId.value, chain.addressOf(hotkey.value))
    }
}
