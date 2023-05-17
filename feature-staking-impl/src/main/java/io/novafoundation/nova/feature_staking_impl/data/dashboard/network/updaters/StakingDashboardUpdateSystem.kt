package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters

import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingStatsDataSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardOptionUpdated
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterFactory
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.coroutineContext

class RealStakingDashboardUpdateSystem(
    private val stakingStatsDataSource: StakingStatsDataSource,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val updaterFactory: StakingDashboardUpdaterFactory,
    private val sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : StakingDashboardUpdateSystem {

    override val syncedItemsFlow: MutableStateFlow<Set<StakingOptionId>> = MutableStateFlow(emptySet())

    override fun start(): Flow<Updater.SideEffect> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            syncedItemsFlow.emit(emptySet())

            val stakingChains = chainRegistry.stakingChains()

            val statsDeferred = CoroutineScope(coroutineContext).async { stakingStatsDataSource.fetchStakingStats(metaAccount, stakingChains) }

            val updateFlows = stakingChains.flatMap { stakingChain ->
                val sharedRequestsBuilder = sharedRequestsBuilderFactory.create(stakingChain.id)

                val chainUpdates = stakingChain.utilityAsset.supportedStakingOptions().mapNotNull { stakingType ->
                    val updater = updaterFactory.createUpdater(stakingChain, stakingType, metaAccount, statsDeferred)
                        ?: return@mapNotNull null

                    updater.listenForUpdates(sharedRequestsBuilder)
                }

                sharedRequestsBuilder.subscribe(coroutineContext)

                chainUpdates
            }

            updateFlows.merge()
                .filterIsInstance<StakingDashboardOptionUpdated>()
                .onEach {
                    syncedItemsFlow.value = syncedItemsFlow.value.added(it.option)
                }
        }
            .onCompletion { syncedItemsFlow.emit(emptySet()) }
    }

    private suspend fun ChainRegistry.stakingChains(): List<Chain> {
        return findChains { it.utilityAsset.supportedStakingOptions().isNotEmpty() }
    }
}
