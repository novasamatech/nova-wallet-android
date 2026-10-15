package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions

class BittensorPortfolioLoaderTest {

    private val positions = mock(BittensorPositionSource::class.java)

    private val swaps = mock(BittensorSwapSource::class.java)

    private val logger = mock(Logger::class.java)

    private val loader = BittensorPortfolioLoader(positions, swaps, logger)

    private val coldKey: AccountId = ByteArray(32)

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    @Test
    fun `root details do not request valuations for subnet positions`() = runBlocking {
        val stake = BittensorStakePositions(
            root = listOf(BittensorRootPosition(hotkey, BigInteger.TEN)),
            subnets = listOf(BittensorSubnetPosition(BittensorNetuid.Subnet(1), hotkey, BigInteger.ONE))
        )
        `when`(positions.positions("chain", coldKey)).thenReturn(stake)

        val root = loader.load("chain", coldKey, rootOnly = true)

        assertEquals(BigInteger.TEN, root.totalStake)
        assertTrue(root.subnets.isEmpty())
        verify(positions).positions("chain", coldKey)
        verifyNoInteractions(swaps)
    }

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
        `when`(positions.positions("chain", coldKey)).thenReturn(stake)
        `when`(swaps.currentAlphaPrice("chain", subnetId)).thenReturn(BigInteger.TEN)

        val portfolio = loader.load("chain", coldKey)

        assertEquals(stake.subnets, portfolio.subnets)
        assertEquals(mapOf(subnetId to BigInteger.TEN), portfolio.subnetPrices)
        verify(swaps).currentAlphaPrice("chain", subnetId)
        verifyNoMoreInteractions(swaps)
    }

    @Test
    fun `failed price does not discard other subnet prices or report a partial total`() = runBlocking {
        val availableSubnet = BittensorNetuid.Subnet(1)
        val unavailableSubnet = BittensorNetuid.Subnet(2)
        val stake = BittensorStakePositions(
            root = emptyList(),
            subnets = listOf(
                BittensorSubnetPosition(availableSubnet, hotkey, BigInteger.ONE),
                BittensorSubnetPosition(unavailableSubnet, hotkey, BigInteger.ONE)
            )
        )
        val failure = IllegalStateException("Price unavailable")
        `when`(positions.positions("chain", coldKey)).thenReturn(stake)
        `when`(swaps.currentAlphaPrice("chain", availableSubnet)).thenReturn(BigInteger.TEN)
        `when`(swaps.currentAlphaPrice("chain", unavailableSubnet)).thenThrow(failure)

        val portfolio = loader.load("chain", coldKey)

        assertEquals(stake.subnets, portfolio.subnets)
        assertEquals(mapOf(availableSubnet to BigInteger.TEN), portfolio.subnetPrices)
        assertNull(portfolio.totalStake)
        verify(logger).log(failure)
    }

    @Test
    fun `price loading cancellation is propagated`() = runBlocking {
        val subnetId = BittensorNetuid.Subnet(1)
        val stake = BittensorStakePositions(
            root = emptyList(),
            subnets = listOf(BittensorSubnetPosition(subnetId, hotkey, BigInteger.ONE))
        )
        val cancellation = CancellationException("Cancelled")
        `when`(positions.positions("chain", coldKey)).thenReturn(stake)
        `when`(swaps.currentAlphaPrice("chain", subnetId)).thenThrow(cancellation)

        try {
            loader.load("chain", coldKey)
            fail("Expected cancellation to propagate")
        } catch (exception: CancellationException) {
            assertEquals(cancellation.message, exception.message)
        }

        verifyNoInteractions(logger)
    }
}
