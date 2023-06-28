package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.address.get
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.utils.parachainStaking
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.data.model.AccountIdKeyMap
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.activeBonded
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.ChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.CandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindCandidateMetadata
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.bindDelegatorState
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isActive
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings.isStakeEnoughToEarnRewards
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class StakingDashboardParachainStakingUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val stakingStatsFlow: Flow<IndexedValue<MultiChainStakingStats>>,
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) { subscribeToStakingState() }
            .transformLatest { parachainStakingBaseInfo ->
                saveItem(parachainStakingBaseInfo, secondaryInfo = null)
                emit(primarySynced())

                val secondarySyncFlow = stakingStatsFlow.map { (index, stakingStats) ->
                    val secondaryInfo = constructSecondaryInfo(parachainStakingBaseInfo, stakingStats)
                    saveItem(parachainStakingBaseInfo, secondaryInfo)

                    secondarySynced(index)
                }

                emitAll(secondarySyncFlow)
            }
    }

    private suspend fun StorageQueryContext.subscribeToStakingState(): Flow<ParachainStakingBaseInfo?> {
        val accountId = metaAccount.accountIdIn(chain) ?: return flowOf(null)

        val delegatorStateFlow = runtime.metadata.parachainStaking().storage("DelegatorState").observe(
            accountId,
            binding = { bindDelegatorState(it, accountId, chain, chainAsset) }
        )

        return delegatorStateFlow.map { delegatorState ->
            if (delegatorState is DelegatorState.Delegator) {
                val delegationKeys = delegatorState.delegations.map { listOf(it.owner) }

                val collatorMetadatas = remoteStorageSource.query(chain.id) {
                    runtime.metadata.parachainStaking().storage("CandidateInfo").entries(
                        keysArguments = delegationKeys,
                        keyExtractor = { (candidateId: AccountId) -> candidateId.intoKey() },
                        binding = { decoded, _ -> bindCandidateMetadata(decoded) }
                    )
                }

                ParachainStakingBaseInfo(delegatorState, collatorMetadatas)
            } else {
                null
            }
        }
    }

    private suspend fun saveItem(
        parachainStakingBaseInfo: ParachainStakingBaseInfo?,
        secondaryInfo: ParachainStakingSecondaryInfo?
    ) = stakingDashboardCache.update { fromCache ->
        if (parachainStakingBaseInfo != null) {
            StakingDashboardItemLocal.staking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                stake = parachainStakingBaseInfo.delegatorState.activeBonded,
                status = secondaryInfo?.status ?: fromCache?.status,
                rewards = secondaryInfo?.rewards ?: fromCache?.rewards,
                estimatedEarnings = secondaryInfo?.estimatedEarnings ?: fromCache?.estimatedEarnings,
                primaryStakingAccountId = parachainStakingBaseInfo.delegatorState.accountId
            )
        } else {
            StakingDashboardItemLocal.notStaking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                estimatedEarnings = secondaryInfo?.estimatedEarnings ?: fromCache?.estimatedEarnings
            )
        }
    }

    private fun constructSecondaryInfo(
        baseInfo: ParachainStakingBaseInfo?,
        multiChainStakingStats: MultiChainStakingStats,
    ): ParachainStakingSecondaryInfo? {
        val chainStakingStats = multiChainStakingStats[stakingOptionId()] ?: return null

        return ParachainStakingSecondaryInfo(
            rewards = chainStakingStats.rewards,
            estimatedEarnings = chainStakingStats.estimatedEarnings.value,
            status = determineStakingStatus(baseInfo, chainStakingStats)
        )
    }

    private fun determineStakingStatus(
        baseInfo: ParachainStakingBaseInfo?,
        chainStakingStats: ChainStakingStats,
    ): StakingDashboardItemLocal.Status? {
        return when {
            baseInfo == null -> null
            chainStakingStats.accountPresentInActiveStakers -> StakingDashboardItemLocal.Status.ACTIVE
            baseInfo.hasWaitingCollators() -> StakingDashboardItemLocal.Status.WAITING
            else -> StakingDashboardItemLocal.Status.INACTIVE
        }
    }

    private fun ParachainStakingBaseInfo.hasWaitingCollators(): Boolean {
        return delegatorState.delegations.any { delegatorBond ->
            val delegateMetadata = delegatesMetadata[delegatorBond.owner]

            delegateMetadata != null && delegateMetadata.isActive && delegateMetadata.isStakeEnoughToEarnRewards(delegatorBond.balance)
        }
    }
}

private class ParachainStakingBaseInfo(
    val delegatorState: DelegatorState.Delegator,
    val delegatesMetadata: AccountIdKeyMap<CandidateMetadata>
) {

    companion object
}

private class ParachainStakingSecondaryInfo(
    val rewards: Balance,
    val estimatedEarnings: Double,
    val status: StakingDashboardItemLocal.Status?
)
