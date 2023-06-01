package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters

import io.novafoundation.nova.common.utils.inserted
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.data.dashboard.SyncingStageMap
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.common.stakingChains
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingStatsDataSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterEvent
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterEvent.PrimaryStakingAccountResolved
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterEvent.SyncingStageUpdated
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterFactory
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ethereum.subscribe
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
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

    override val syncedItemsFlow: MutableStateFlow<SyncingStageMap> = MutableStateFlow(emptyMap())

    private val resolvedPrimaryStakingAccountsFlow = MutableStateFlow(emptyMap<StakingOptionId, AccountId?>())

    override fun start(): Flow<Updater.SideEffect> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            syncedItemsFlow.emit(emptyMap())
            resolvedPrimaryStakingAccountsFlow.emit(emptyMap())

            val stakingChains = chainRegistry.stakingChains()
            val supportedOptionsSize = stakingChains.sumOf { it.utilityAsset.supportedStakingOptions().size }

            val statsDeferred = CoroutineScope(coroutineContext).async {
                val resolvedPrimaryStakingAccounts = resolvedPrimaryStakingAccountsFlow.awaitSize(supportedOptionsSize)
                stakingStatsDataSource.fetchStakingStats(resolvedPrimaryStakingAccounts, stakingChains)
            }

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
                .filterIsInstance<StakingDashboardUpdaterEvent>()
                .onEach(::handleUpdaterEvent)
        }
            .onCompletion {
                syncedItemsFlow.emit(emptyMap())
                resolvedPrimaryStakingAccountsFlow.emit(emptyMap())
            }
    }

    private fun handleUpdaterEvent(event: StakingDashboardUpdaterEvent) {
        when (event) {
            is PrimaryStakingAccountResolved -> {
                resolvedPrimaryStakingAccountsFlow.value = resolvedPrimaryStakingAccountsFlow.value.inserted(event.option, event.primaryAccount)
            }
            is SyncingStageUpdated -> {
                syncedItemsFlow.value = syncedItemsFlow.value.inserted(event.option, event.syncingStage)
            }
        }
    }

    private suspend fun <K, V> Flow<Map<K, V>>.awaitSize(size: Int): Map<K, V> {
        return first { it.size >= size }
    }
}
