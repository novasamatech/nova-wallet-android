package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.common.utils.inserted
import io.novafoundation.nova.common.utils.mapIndexed
import io.novafoundation.nova.common.utils.zipWithPrevious
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.data.dashboard.StakingDashboardUpdateSystem
import io.novafoundation.nova.feature_staking_api.data.dashboard.SyncingStageMap
import io.novafoundation.nova.feature_staking_api.data.dashboard.getSyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.SyncingStage
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.common.stakingChains
import io.novafoundation.nova.feature_staking_impl.data.dashboard.model.StakingDashboardPrimaryAccount
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingAccounts
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.StakingStatsDataSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterEvent
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain.StakingDashboardUpdaterFactory
import io.novafoundation.nova.feature_staking_impl.data.dashboard.repository.StakingDashboardRepository
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.ext.supportedStakingOptions
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.withIndex
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private const val EMPTY_OFF_CHAIN_SYNC_INDEX = -1

class RealStakingDashboardUpdateSystem(
    private val stakingStatsDataSource: StakingStatsDataSource,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val updaterFactory: StakingDashboardUpdaterFactory,
    private val sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val stakingDashboardRepository: StakingDashboardRepository,
    private val offChainSyncDebounceRate: Duration = 2.seconds
) : StakingDashboardUpdateSystem {

    override val syncedItemsFlow: MutableStateFlow<SyncingStageMap> = MutableStateFlow(emptyMap())
    private val latestOffChainSyncIndex: MutableStateFlow<Int> = MutableStateFlow(EMPTY_OFF_CHAIN_SYNC_INDEX)

    override fun start(): Flow<Updater.SideEffect> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            val accountScope = CoroutineScope(coroutineContext)

            syncedItemsFlow.emit(emptyMap())
            latestOffChainSyncIndex.emit(EMPTY_OFF_CHAIN_SYNC_INDEX)

            val stakingChains = chainRegistry.stakingChains()
            val stakingOptionsWithChain = stakingChains.associateWithStakingOptions()

            val offChainSyncFlow = debouncedOffChainSyncFlow(metaAccount, stakingOptionsWithChain, stakingChains)
                .shareIn(accountScope, started = SharingStarted.Eagerly, replay = 1)

            val updateFlows = stakingChains.flatMap { stakingChain ->
                val sharedRequestsBuilder = sharedRequestsBuilderFactory.create(stakingChain.id)

                val chainUpdates = stakingChain.utilityAsset.supportedStakingOptions().mapNotNull { stakingType ->
                    val updater = updaterFactory.createUpdater(stakingChain, stakingType, metaAccount, offChainSyncFlow)
                        ?: return@mapNotNull null

                    updater.listenForUpdates(sharedRequestsBuilder)
                }

                sharedRequestsBuilder.subscribe(accountScope)

                chainUpdates
            }

            updateFlows.merge()
                .filterIsInstance<StakingDashboardUpdaterEvent>()
                .onEach(::handleUpdaterEvent)
        }
            .onCompletion {
                syncedItemsFlow.emit(emptyMap())
            }
    }

    private fun debouncedOffChainSyncFlow(
        metaAccount: MetaAccount,
        stakingOptionsWithChain: Map<StakingOptionId, Chain>,
        stakingChains: List<Chain>
    ): Flow<IndexedValue<MultiChainStakingStats>> {
        return stakingDashboardRepository.stakingAccountsFlow(metaAccount.id)
            .map { stakingPrimaryAccounts -> constructStakingAccounts(stakingOptionsWithChain, metaAccount, stakingPrimaryAccounts) }
            .zipWithPrevious()
            .transform { (previousAccounts, currentAccounts) ->
                if (previousAccounts != null) {
                    val diff = CollectionDiffer.findDiff(previousAccounts, currentAccounts, forceUseNewItems = false)
                    if (diff.newOrUpdated.isNotEmpty()) {
                        markSyncingSecondaryFor(diff.newOrUpdated)
                        emit(currentAccounts)
                    }
                } else {
                    emit(currentAccounts)
                }
            }
            .withIndex()
            .onEach { latestOffChainSyncIndex.value = it.index }
            .debounce { (index) -> if (index == 0) 0.milliseconds else offChainSyncDebounceRate }
            .mapIndexed { stakingAccounts -> stakingStatsDataSource.fetchStakingStats(stakingAccounts, stakingChains) }
    }

    private fun markSyncingSecondaryFor(changedPrimaryAccounts: List<Map.Entry<StakingOptionId, AccountIdKey?>>) {
        val result = syncedItemsFlow.value.toMutableMap()

        changedPrimaryAccounts.forEach { (stakingOptionId, _) ->
            result[stakingOptionId] = result.getSyncingStage(stakingOptionId).coerceAtMost(SyncingStage.SYNCING_SECONDARY)
        }

        syncedItemsFlow.value = result
    }

    private fun List<Chain>.associateWithStakingOptions(): Map<StakingOptionId, Chain> {
        return flatMap { chain ->
            chain.assets.flatMap { asset ->
                asset.supportedStakingOptions().map {
                    StakingOptionId(chain.id, asset.id, it) to chain
                }
            }
        }.toMap()
    }

    private fun constructStakingAccounts(
        stakingOptionIds: Map<StakingOptionId, Chain>,
        metaAccount: MetaAccount,
        knownPrimaryAccounts: List<StakingDashboardPrimaryAccount>
    ): StakingAccounts {
        val knownPrimaryAccountsByOptionId = knownPrimaryAccounts.associateBy(StakingDashboardPrimaryAccount::stakingOptionId)

        return stakingOptionIds.mapValues { (optionId, chain) ->
            val accountId = knownPrimaryAccountsByOptionId[optionId]?.primaryStakingAccountId?.value
                ?: metaAccount.accountIdIn(chain)

            accountId?.intoKey()
        }
    }

    private fun handleUpdaterEvent(event: StakingDashboardUpdaterEvent) {
        when (event) {
            is StakingDashboardUpdaterEvent.AllSynced -> {
                // we only mark option as synced if there are no fresher syncs
                if (event.indexOfUsedOffChainSync >= latestOffChainSyncIndex.value) {
                    syncedItemsFlow.value = syncedItemsFlow.value.inserted(event.option, SyncingStage.SYNCED)
                }
            }
            is StakingDashboardUpdaterEvent.PrimarySynced -> {
                syncedItemsFlow.value = syncedItemsFlow.value.inserted(event.option, SyncingStage.SYNCING_SECONDARY)
            }
        }
    }
}
