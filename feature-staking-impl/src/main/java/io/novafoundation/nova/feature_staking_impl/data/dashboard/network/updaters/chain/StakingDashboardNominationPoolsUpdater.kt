package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.bondedPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.nominationPools
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.api.poolMembers
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.BondedPool
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolPoints
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolBalanceRepository
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolBalanceConvertable
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.amountOf
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class StakingDashboardNominationPoolsUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val stakingStatsFlow: Flow<IndexedValue<MultiChainStakingStats>>,
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource,
    private val nominationPoolBalanceRepository: NominationPoolBalanceRepository,
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) { subscribeToStakingState() }
            .transformLatest { onChainInfo ->
                saveItem(onChainInfo)
                emit(primarySynced())

                // TODO sync with subQuery
            }
    }

    private suspend fun StorageQueryContext.subscribeToStakingState(): Flow<PoolsOnChainInfo?> {
        val accountId = metaAccount.accountIdIn(chain) ?: return flowOf(null)

        val poolMemberFlow = metadata.nominationPools.poolMembers.observe(accountId)

        val pollWithBalanceFlow = poolMemberFlow
            .map { it?.poolId }
            .distinctUntilChanged()
            .flatMapLatest(::subscribeToPoolWithBalance)

        return combine(poolMemberFlow, pollWithBalanceFlow) { poolMember, poolWithBalance ->
            if (poolMember != null && poolWithBalance != null) {
                PoolsOnChainInfo(poolMember, poolWithBalance)
            } else {
                null
            }
        }
    }

    private suspend fun subscribeToPoolWithBalance(poolId: PoolId?): Flow<PoolWithBalance?> {
        if (poolId == null) return flowOf(null)

        return remoteStorageSource.subscribeBatched(chain.id) {
            combine(
                metadata.nominationPools.bondedPools.observeNonNull(poolId.value),
                nominationPoolBalanceRepository.observeBondedBalance(poolId),
                ::PoolWithBalance
            )
        }
    }


    private suspend fun saveItem(
        relaychainStakingBaseInfo: PoolsOnChainInfo?,
    ) = stakingDashboardCache.update { fromCache ->
        if (relaychainStakingBaseInfo != null) {
            StakingDashboardItemLocal.staking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                stake = relaychainStakingBaseInfo.stakedBalance(),
                status = fromCache?.status,
                rewards = fromCache?.rewards,
                estimatedEarnings = fromCache?.estimatedEarnings,
                primaryStakingAccountId = relaychainStakingBaseInfo.poolMember.accountId,
            )
        } else {
            StakingDashboardItemLocal.notStaking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                estimatedEarnings = fromCache?.estimatedEarnings
            )
        }
    }

    private class PoolsOnChainInfo(val poolMember: PoolMember, val poolWithBalance: PoolWithBalance) {

        fun stakedBalance(): Balance {
            return poolWithBalance.amountOf(poolMember.points)
        }
    }

    private class PoolWithBalance(val pool: BondedPool, override val poolBalance: Balance) : PoolBalanceConvertable {

        override val poolPoints: PoolPoints = pool.points
    }
}
