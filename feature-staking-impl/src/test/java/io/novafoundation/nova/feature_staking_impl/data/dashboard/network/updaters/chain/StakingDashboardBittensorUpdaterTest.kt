package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioLoader
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class StakingDashboardBittensorUpdaterTest {

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val metaAccount = mock(MetaAccount::class.java)

    private val positionSource = mock(BittensorPositionSource::class.java)

    private val swapSource = mock(BittensorSwapSource::class.java)

    private val chainStateRepository = mock(ChainStateRepository::class.java)

    private val subscriptionBuilder = mock(SharedRequestsBuilder::class.java)

    private val blockNumbers = MutableStateFlow(BigInteger.ONE)

    private val cache = RecordingCache()

    private val coldKey: AccountId = ByteArray(32)

    @Test
    fun `registers subscription before collection and updates stake from new blocks`() = runBlocking {
        prepare()
        val updates = updater().listenForUpdates(subscriptionBuilder)
        verify(chainStateRepository).currentRemoteBlockNumberFlow(chain.id, subscriptionBuilder)

        val stakes = mutableListOf<BigInteger?>()
        withTimeout(3_000L) {
            updates.onEach {
                stakes += cache.item?.stake
                `when`(positionSource.positions(chain.id, coldKey)).thenReturn(BittensorStakePositions(emptyList(), emptyList()))
                blockNumbers.value += BigInteger.ONE
            }.take(2).toList()
        }

        assertEquals(listOf(BigInteger.TEN, null), stakes)
        assertFalse(requireNotNull(cache.item).hasStake)
        verifyNoInteractions(swapSource)
    }

    @Test
    fun `wallet without a chain account does not subscribe or query positions`() = runBlocking {
        prepare()
        `when`(metaAccount.accountIdIn(chain)).thenReturn(null)

        updater().listenForUpdates(subscriptionBuilder).first()

        assertFalse(requireNotNull(cache.item).hasStake)
        verifyNoInteractions(chainStateRepository, positionSource, swapSource)
    }

    private suspend fun prepare() {
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(metaAccount.id).thenReturn(1L)
        `when`(metaAccount.accountIdIn(chain)).thenReturn(coldKey)
        `when`(chainStateRepository.currentRemoteBlockNumberFlow(chain.id, subscriptionBuilder)).thenReturn(blockNumbers)
        `when`(positionSource.positions(chain.id, coldKey)).thenReturn(
            BittensorStakePositions(listOf(BittensorRootPosition(AccountIdKey(ByteArray(32)), BigInteger.TEN)), emptyList())
        )
    }

    private val logger = mock(Logger::class.java)

    private val portfolioLoader = BittensorPortfolioLoader(positionSource, swapSource, logger)

    private fun updater() = StakingDashboardBittensorUpdater(
        chain,
        chainAsset,
        metaAccount,
        portfolioLoader,
        cache,
        chainStateRepository
    )

    private class RecordingCache : StakingDashboardCache {

        var item: StakingDashboardItemLocal? = null

        override suspend fun update(
            chainId: String,
            assetId: Int,
            stakingTypeLocal: String,
            metaAccountId: Long,
            updating: (StakingDashboardItemLocal?) -> StakingDashboardItemLocal
        ) {
            item = updating(item)
        }
    }
}
