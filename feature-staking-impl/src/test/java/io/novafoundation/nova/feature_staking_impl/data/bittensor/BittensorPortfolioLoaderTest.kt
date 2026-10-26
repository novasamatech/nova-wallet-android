package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

class BittensorPortfolioLoaderTest {

    private val prices = mock(BittensorPortfolioPriceRepository::class.java)

    private val logger = mock(Logger::class.java)

    private val loader = BittensorPortfolioLoader(prices, logger)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    @Test
    fun `price is loaded once for multiple positions in the same subnet`() = runBlocking {
        val subnetId = BittensorNetuid.Subnet(1)
        val anotherHotkey = AccountIdKey(ByteArray(32) { 2 })
        val stake = BittensorStakePositions(
            root = emptyList(),
            subnets = listOf(
                BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE),
                BittensorSubnetPosition(subnetId, anotherHotkey, BigInteger.TEN)
            )
        )
        `when`(prices.prices("chain", loader.heldSubnets(stake))).thenReturn(mapOf(subnetId to BigInteger.TEN))

        val portfolio = load(stake)

        assertEquals(stake.subnets, portfolio.subnets)
        assertEquals(mapOf(subnetId to BigInteger.TEN), portfolio.subnetPrices)
        verify(prices).prices("chain", loader.heldSubnets(stake))
        verifyNoMoreInteractions(prices)
    }

    @Test
    fun `missing price does not discard other subnet prices or report a partial total`() = runBlocking {
        val availableSubnet = BittensorNetuid.Subnet(1)
        val unavailableSubnet = BittensorNetuid.Subnet(2)
        val stake = BittensorStakePositions(
            root = emptyList(),
            subnets = listOf(
                BittensorSubnetPosition(availableSubnet, hotkey, BigInteger.ONE),
                BittensorSubnetPosition(unavailableSubnet, hotkey, BigInteger.ONE)
            )
        )
        `when`(prices.prices("chain", loader.heldSubnets(stake))).thenReturn(mapOf(availableSubnet to BigInteger.TEN))

        val portfolio = load(stake)

        assertEquals(stake.subnets, portfolio.subnets)
        assertEquals(mapOf(availableSubnet to BigInteger.TEN), portfolio.subnetPrices)
        assertNull(portfolio.totalStake)
        verifyNoInteractions(logger)
    }

    @Test
    fun `price loading cancellation is propagated`() = runBlocking {
        val subnetId = BittensorNetuid.Subnet(1)
        val stake = BittensorStakePositions(
            root = emptyList(),
            subnets = listOf(BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE))
        )
        val cancellation = CancellationException("Cancelled")
        `when`(prices.prices("chain", loader.heldSubnets(stake))).thenThrow(cancellation)

        try {
            load(stake)
            fail("Expected cancellation to propagate")
        } catch (exception: CancellationException) {
            assertEquals(cancellation.message, exception.message)
        }

        verifyNoInteractions(logger)
    }

    @Test
    fun `backend failure preserves holdings with unknown valuation`() = runBlocking {
        val subnetId = BittensorNetuid.Subnet(1)
        val position = BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE)
        val stake = BittensorStakePositions(emptyList(), listOf(position))
        val failure = IllegalStateException("Catalogue unavailable")
        `when`(prices.prices("chain", loader.heldSubnets(stake))).thenThrow(failure)

        val portfolio = load(stake)

        assertEquals(stake.subnets, portfolio.subnets)
        assertNull(portfolio.totalStake)
        verify(logger).log(argThat<Throwable> { it.message == failure.message })
    }

    private suspend fun load(stake: BittensorStakePositions): io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions {
        val prices = loader.loadPrices("chain", loader.heldSubnets(stake))
        return loader.value(stake, prices)
    }

    @Test
    fun `empty portfolio does not request subnet prices`() = runBlocking {
        val stake = BittensorStakePositions(emptyList(), emptyList())

        val portfolio = load(stake)

        assertEquals(BigInteger.ZERO, portfolio.totalStake)
        verifyNoInteractions(prices)
    }

    @Test
    fun `root-only holdings are valued without pricing dependencies`() = runBlocking {
        val root = BittensorRootPosition(hotkey, BigInteger.TEN)
        val stake = BittensorStakePositions(listOf(root), emptyList())

        assertEquals(BigInteger.TEN, load(stake).totalStake)
        verifyNoInteractions(prices)
    }
}
