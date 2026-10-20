package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioLoader
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimPreview
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaims
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioPriceRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.RealBittensorPositionRepository
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class StakingDashboardBittensorUpdaterTest {

    private val chain = mock(Chain::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val metaAccount = mock(MetaAccount::class.java)

    private val positionSource = mock(BittensorPositionSource::class.java)

    private val rootClaims = mock(BittensorRootClaimSource::class.java)

    private val priceRepository = mock(BittensorPortfolioPriceRepository::class.java)

    private val chainStateRepository = mock(ChainStateRepository::class.java)

    private val subscriptionBuilder = mock(SharedRequestsBuilder::class.java)

    private val blockNumbers = MutableStateFlow(BigInteger.ONE)

    private val cache = RecordingCache()

    private val coldKey: AccountId = ByteArray(32)

    @Test
    fun `updates dashboard from the shared position stream`() = runBlocking {
        prepare()
        val updates = updater().listenForUpdates(subscriptionBuilder)

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
        verifyNoInteractions(priceRepository)
    }

    @Test
    fun `wallet without a chain account does not subscribe or query positions`() = runBlocking {
        prepare()
        `when`(metaAccount.accountIdIn(chain)).thenReturn(null)

        updater().listenForUpdates(subscriptionBuilder).first()

        assertFalse(requireNotNull(cache.item).hasStake)
        verifyNoInteractions(chainStateRepository, positionSource, priceRepository)
    }

    private suspend fun prepare() {
        `when`(chain.id).thenReturn(ChainGeneses.BITTENSOR)
        `when`(metaAccount.id).thenReturn(1L)
        `when`(metaAccount.accountIdIn(chain)).thenReturn(coldKey)
        `when`(rootClaims.claims(chain.id, coldKey)).thenReturn(BittensorRootClaims(emptyList(), BigInteger.ONE))
        `when`(chainStateRepository.currentRemoteBlockNumberFlow(chain.id)).thenReturn(blockNumbers)
        `when`(positionSource.positions(chain.id, coldKey)).thenReturn(
            BittensorStakePositions(listOf(BittensorRootPosition(AccountIdKey(ByteArray(32)), BigInteger.TEN)), emptyList())
        )
    }

    private val logger = mock(Logger::class.java)

    private val portfolioLoader = BittensorPortfolioLoader(priceRepository, logger)

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val positionUpdates = io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPositionUpdates()

    private val positionRepository = RealBittensorPositionRepository(
        positionSource,
        portfolioLoader,
        chainStateRepository,
        computationalCache,
        logger,
        positionUpdates
    )

    private fun updater() = StakingDashboardBittensorUpdater(
        chain,
        chainAsset,
        metaAccount,
        positionRepository,
        cache,
        rootClaims,
        logger
    )

    @Test
    fun `redeemable rewards keep zero stake wallet in staking dashboard`() = runBlocking {
        prepare()
        `when`(positionSource.positions(chain.id, coldKey)).thenReturn(BittensorStakePositions(emptyList(), emptyList()))
        val preview = BittensorRootClaimPreview(AccountIdKey(ByteArray(32)), BigInteger.TEN, BigInteger.ONE, BigInteger.ZERO)
        val claims = BittensorRootClaims(listOf(preview), BigInteger.TEN)
        `when`(rootClaims.claims(chain.id, coldKey)).thenReturn(claims)

        withTimeout(3_000L) { updater().listenForUpdates(subscriptionBuilder).first() }

        assertTrue(requireNotNull(cache.item).hasStake)
        assertEquals(BigInteger.ZERO, cache.item?.stake)
    }

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
