package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.mapOptional
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.takeUnlessZero
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.ChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.MultiChainOffChainSyncResult
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.candidateStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.collatorStaking
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.api.userStake
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.MythDelegation
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.SessionIndex
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.UserStakeInfo
import io.novafoundation.nova.feature_staking_impl.data.mythos.network.blockchain.model.hasActiveCollators
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.observeMythosLocks
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.total
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.currentIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.session
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.validators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.SessionValidators
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.cache.StorageCachingContext
import io.novafoundation.nova.runtime.storage.cache.cacheValues
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlin.coroutines.coroutineContext

class StakingDashboardMythosUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val stakingDashboardCache: StakingDashboardCache,
    override val storageCache: StorageCache,
    private val remoteStorageSource: StorageDataSource,
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount),
    StorageCachingContext by StorageCachingContext(storageCache) {

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder
    ): Flow<Updater.SideEffect> {
        return subscribeToOnChainState(storageSubscriptionBuilder).transformLatest { onChainState ->
            saveItem(onChainState, secondaryInfo = null)
            emit(primarySynced())

            val secondarySyncFlow = stakingStatsFlow.map { (index, stakingStats) ->
                val secondaryInfo = constructSecondaryInfo(onChainState, stakingStats)
                saveItem(onChainState, secondaryInfo)

                secondarySynced(index)
            }

            emitAll(secondarySyncFlow)
        }
    }

    private suspend fun subscribeToOnChainState(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<OnChainInfo?> {
        val accountId = metaAccount.accountIdKeyIn(chain) ?: return flowOf(null)

        val userStakeShared = subscribeToUserStake(storageSubscriptionBuilder, accountId)
            .shareIn(CoroutineScope(coroutineContext), SharingStarted.Lazily, replay = 1)

        return combine(
            subscribeToTotalStake(),
            userStakeShared,
            sessionValidatorsFlow(storageSubscriptionBuilder)
        ) { totalStake, userStakeInfo, sessionValidators ->
            constructOnChainInfo(totalStake, userStakeInfo, accountId, sessionValidators)
        }
    }

    private suspend fun sessionValidatorsFlow(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Set<AccountIdKey>> {
        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) {
            metadata.session.validators.observeNonNull()
        }
    }

    private fun constructOnChainInfo(
        totalStake: Balance?,
        userStakeInfo: UserStakeInfo?,
        accountId: AccountIdKey,
        sessionValidators: SessionValidators,
    ): OnChainInfo? {
        if (totalStake == null) return null

        val hasActiveValidators = userStakeInfo.hasActiveCollators(sessionValidators)
        val activeStake = userStakeInfo?.balance.orZero()

        return OnChainInfo(activeStake, accountId, hasActiveValidators)
    }

    private fun constructSecondaryInfo(
        baseInfo: OnChainInfo?,
        multiChainStakingStats: MultiChainStakingStats,
    ): SecondaryInfo? {
        val chainStakingStats = multiChainStakingStats[stakingOptionId()] ?: return null

        return SecondaryInfo(
            rewards = chainStakingStats.rewards,
            estimatedEarnings = chainStakingStats.estimatedEarnings.value,
            status = determineStakingStatus(baseInfo)
        )
    }

    private fun determineStakingStatus(baseInfo: OnChainInfo?): StakingDashboardItemLocal.Status? {
        return when {
            baseInfo == null -> null
            baseInfo.activeStake.isZero -> StakingDashboardItemLocal.Status.INACTIVE
            baseInfo.hasActiveCollators -> StakingDashboardItemLocal.Status.ACTIVE
            else -> StakingDashboardItemLocal.Status.INACTIVE
        }
    }

    private fun subscribeToTotalStake(): Flow<Balance?> {
        return balanceLocksRepository.observeMythosLocks(metaAccount.id, chain, chainAsset).map { mythosLocks ->
            mythosLocks.total.takeUnlessZero()
        }
    }

    private suspend fun subscribeToUserStake(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        accountId: AccountIdKey
    ): Flow<UserStakeInfo?> {
        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) {
            metadata.collatorStaking.userStake.observeWithRaw(accountId.value)
                .cacheValues()
        }
    }

    private suspend fun saveItem(
        onChainInfo: OnChainInfo?,
        secondaryInfo: SecondaryInfo?
    ) = stakingDashboardCache.update { fromCache ->
        if (onChainInfo != null) {
            StakingDashboardItemLocal.staking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                stake = onChainInfo.activeStake,
                status = secondaryInfo?.status ?: fromCache?.status,
                rewards = secondaryInfo?.rewards ?: fromCache?.rewards,
                estimatedEarnings = secondaryInfo?.estimatedEarnings ?: fromCache?.estimatedEarnings,
                stakeStatusAccount = onChainInfo.accountId.value,
                rewardsAccount = onChainInfo.accountId.value
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

    private class OnChainInfo(
        val activeStake: Balance,
        val accountId: AccountIdKey,
        val hasActiveCollators: Boolean
    )

    private class SecondaryInfo(
        val rewards: Balance,
        val estimatedEarnings: Double,
        val status: StakingDashboardItemLocal.Status?
    )
}
