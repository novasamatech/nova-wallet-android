package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.activeBalance
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.ChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.MultiChainOffChainSyncResult
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.activeEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.bondedPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.poolMembers
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolBalanceConvertable
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.cache.StorageCachingContext
import io.novafoundation.nova.runtime.storage.cache.cacheValues
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlin.coroutines.coroutineContext

class StakingDashboardNominationPoolsUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource,
    private val nominationPoolStateRepository: NominationPoolStateRepository,
    private val poolAccountDerivation: PoolAccountDerivation,
    storageCache: StorageCache,
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount),
    StorageCachingContext by StorageCachingContext(storageCache) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) {
            val stakingStateFlow = subscribeToStakingState()
            val activeEraFlow = metadata.staking.activeEra
                .observeWithRaw()
                .cacheValues()
                .filterNotNull()

            combineToPair(stakingStateFlow, activeEraFlow)
        }
            .transformLatest { (onChainInfo, activeEra) ->
                saveItem(onChainInfo, secondaryInfo = null)
                emit(primarySynced())

                val secondarySyncFlow = stakingStatsFlow.map { (index, stakingStats) ->
                    val secondaryInfo = constructSecondaryInfo(onChainInfo, activeEra, stakingStats)
                    saveItem(onChainInfo, secondaryInfo)

                    secondarySynced(index)
                }

                emitAll(secondarySyncFlow)
            }
    }

    private suspend fun StorageQueryContext.subscribeToStakingState(): Flow<PoolsOnChainInfo?> {
        val accountId = metaAccount.accountIdIn(chain) ?: return flowOf(null)

        val poolMemberFlow = metadata.nominationPools.poolMembers
            .observeWithRaw(accountId)
            .cacheValues()

        return flowOfAll {
            val poolMemberFlowShared = poolMemberFlow
                .shareIn(CoroutineScope(coroutineContext), SharingStarted.Lazily, replay = 1)

            val poolAggregatedStateFlow = poolMemberFlowShared
                .map { it?.poolId }
                .distinctUntilChanged()
                .flatMapLatest(::subscribeToPoolWithBalance)

            combine(poolMemberFlow, poolAggregatedStateFlow) { poolMember, poolWithBalance ->
                if (poolMember != null && poolWithBalance != null) {
                    PoolsOnChainInfo(poolMember, poolWithBalance)
                } else {
                    null
                }
            }
        }
    }

    private suspend fun subscribeToPoolWithBalance(poolId: PoolId?): Flow<PoolAggregatedState?> {
        if (poolId == null) return flowOf(null)

        val bondedPoolAccountId = poolAccountDerivation.derivePoolAccount(poolId, PoolAccountDerivation.PoolAccountType.BONDED, chain.id)

        return remoteStorageSource.subscribeBatched(chain.id) {
            val bondedPoolFlow = metadata.nominationPools.bondedPools.observeWithRaw(poolId.value)
                .cacheValues()
                .filterNotNull()

            val poolNominationsFlow = nominationPoolStateRepository.observePoolNominations(bondedPoolAccountId)
                .cacheValues()

            val activeStakeFlow = nominationPoolStateRepository.observeBondedPoolLedger(bondedPoolAccountId)
                .cacheValues()
                .map { it.activeBalance() }

            combine(
                bondedPoolFlow,
                poolNominationsFlow,
                activeStakeFlow,
            ) { bondedPool, nominations, balance ->
                PoolAggregatedState(bondedPool, nominations, balance, bondedPoolAccountId)
            }
        }
    }

    private suspend fun saveItem(
        relaychainStakingBaseInfo: PoolsOnChainInfo?,
        secondaryInfo: NominationPoolsSecondaryInfo?,
    ) = stakingDashboardCache.update { fromCache ->
        if (relaychainStakingBaseInfo != null) {
            StakingDashboardItemLocal.staking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                stake = relaychainStakingBaseInfo.stakedBalance(),
                status = secondaryInfo?.status ?: fromCache?.status,
                rewards = secondaryInfo?.rewards ?: fromCache?.rewards,
                estimatedEarnings = secondaryInfo?.estimatedEarnings ?: fromCache?.estimatedEarnings,
                stakeStatusAccount = relaychainStakingBaseInfo.poolAggregatedState.poolStash,
                rewardsAccount = relaychainStakingBaseInfo.poolMember.accountId
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
        baseInfo: PoolsOnChainInfo?,
        activeEra: EraIndex,
        multiChainStakingStats: MultiChainStakingStats,
    ): NominationPoolsSecondaryInfo? {
        val chainStakingStats = multiChainStakingStats[stakingOptionId()] ?: return null

        return NominationPoolsSecondaryInfo(
            rewards = chainStakingStats.rewards,
            estimatedEarnings = chainStakingStats.estimatedEarnings.value,
            status = determineStakingStatus(baseInfo, activeEra, chainStakingStats)
        )
    }

    private fun determineStakingStatus(
        baseInfo: PoolsOnChainInfo?,
        activeEra: EraIndex,
        chainStakingStats: ChainStakingStats,
    ): StakingDashboardItemLocal.Status? {
        return when {
            baseInfo == null -> null
            baseInfo.poolMember.points.value.isZero -> StakingDashboardItemLocal.Status.INACTIVE
            chainStakingStats.accountPresentInActiveStakers -> StakingDashboardItemLocal.Status.ACTIVE
            baseInfo.poolAggregatedState.poolNominations != null && baseInfo.poolAggregatedState.poolNominations.isWaiting(activeEra) -> {
                StakingDashboardItemLocal.Status.WAITING
            }
            else -> StakingDashboardItemLocal.Status.INACTIVE
        }
    }

    private class PoolsOnChainInfo(
        val poolMember: PoolMember,
        val poolAggregatedState: PoolAggregatedState
    ) {

        fun stakedBalance(): Balance {
            return poolAggregatedState.amountOf(poolMember.points)
        }
    }

    private class PoolAggregatedState(
        val pool: BondedPool,
        val poolNominations: Nominations?,
        override val poolBalance: Balance,
        val poolStash: AccountId
    ) : PoolBalanceConvertable {

        override val poolPoints: PoolPoints = pool.points
    }

    private class NominationPoolsSecondaryInfo(
        val rewards: Balance,
        val estimatedEarnings: Double,
        val status: StakingDashboardItemLocal.Status?
    )
}
