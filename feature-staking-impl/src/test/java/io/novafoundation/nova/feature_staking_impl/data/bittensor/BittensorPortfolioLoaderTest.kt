package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

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
            subnets = listOf(BittensorSubnetPosition(BittensorSubnetId(1), hotkey, BigInteger.ONE))
        )
        `when`(positions.positions("chain", coldKey)).thenReturn(stake)

        val root = loader.load("chain", coldKey, rootOnly = true)

        assertEquals(BigInteger.TEN, root.totalStake)
        assertTrue(root.subnets.isEmpty())
        verify(positions).positions("chain", coldKey)
        verifyNoInteractions(swaps)
    }
}
