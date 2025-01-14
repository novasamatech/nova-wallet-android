package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.utils.findById
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythosStakingFreezeIds
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class StakingDashboardMythosUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val stakingDashboardCache: StakingDashboardCache,
    ): BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount) {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder
    ): Flow<Updater.SideEffect> {
        return subscribeToOnChainState().transformLatest { onChainState ->
            saveItem(onChainState)
            emit(primarySynced())

            // TODO implement secondary sync
            emit(secondarySynced(1000))
        }
    }

    private fun subscribeToOnChainState(): Flow<OnChainInfo?> {
        val accountId = metaAccount.accountIdIn(chain) ?: return flowOf(null)

        return balanceLocksRepository.observeBalanceLocks(metaAccount.id, chain, chainAsset).map {  locks ->
           locks.getTotalStake()?.let { OnChainInfo(it, accountId) }
        }
    }

    private suspend fun saveItem(
        onChainInfo: OnChainInfo?
    ) = stakingDashboardCache.update { fromCache ->
        if (onChainInfo != null) {
            StakingDashboardItemLocal.staking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                stake = onChainInfo.totalStake,
                status = fromCache?.status,
                rewards = fromCache?.rewards,
                estimatedEarnings = fromCache?.estimatedEarnings,
                stakeStatusAccount = onChainInfo.accountId,
                rewardsAccount = onChainInfo.accountId
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

    private fun List<BalanceLock>.getTotalStake(): Balance? {
        val stakingLock = findById(MythosStakingFreezeIds.STAKING)
        val releasingLock = findById(MythosStakingFreezeIds.RELEASING)

        if (stakingLock == null && releasingLock == null) return null

        return stakingLock?.amountInPlanks.orZero() + releasingLock?.amountInPlanks.orZero()
    }

    private class OnChainInfo(val totalStake: Balance, val accountId: AccountId)
}
