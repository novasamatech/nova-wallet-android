@file:OptIn(ExperimentalCoroutinesApi::class)

package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal.Status
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_api.domain.model.ValidatorPrefs
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.ChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.MultiChainOffChainSyncResult
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.activeEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.bonded
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.ledger
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.nominators
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.validators
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.metadata
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

class StakingDashboardRelayStakingUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        val accountId = metaAccount.accountIdIn(chain)

        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) {
            val activeEraFlow = metadata.staking.activeEra.observeNonNull()

            val baseInfo = if (accountId != null) {
                val bondedFlow = metadata.staking.bonded.observe(accountId)

                bondedFlow.flatMapLatest { maybeController ->
                    val controllerId = maybeController ?: accountId

                    subscribeToStakingState(controllerId)
                }
            } else {
                flowOf(null)
            }

            combineToPair(baseInfo, activeEraFlow)
        }.transformLatest { (relaychainStakingState, activeEra) ->
            saveItem(relaychainStakingState, secondaryInfo = null)
            emit(primarySynced())

            val secondarySyncFlow = stakingStatsFlow.map { (index, stakingStats) ->
                val secondaryInfo = constructSecondaryInfo(relaychainStakingState, activeEra, stakingStats)
                saveItem(relaychainStakingState, secondaryInfo)

                secondarySynced(index)
            }

            emitAll(secondarySyncFlow)
        }
    }

    private fun subscribeToStakingState(controllerId: AccountId): Flow<RelaychainStakingBaseInfo?> {
        return remoteStorageSource.subscribe(chain.id) {
            metadata.staking.ledger.observe(controllerId).flatMapLatest { ledger ->
                if (ledger != null) {
                    subscribeToStakerIntentions(ledger.stashId).map { (nominations, validatorPrefs) ->
                        RelaychainStakingBaseInfo(ledger, nominations, validatorPrefs)
                    }
                } else {
                    flowOf(null)
                }
            }
        }
    }

    private suspend fun subscribeToStakerIntentions(stashId: AccountId): Flow<Pair<Nominations?, ValidatorPrefs?>> {
        return remoteStorageSource.subscribeBatched(chain.id) {
            combineToPair(
                metadata.staking.nominators.observe(stashId),
                metadata.staking.validators.observe(stashId)
            )
        }
    }

    private suspend fun saveItem(
        relaychainStakingBaseInfo: RelaychainStakingBaseInfo?,
        secondaryInfo: RelaychainStakingSecondaryInfo?
    ) = stakingDashboardCache.update { fromCache ->
        if (relaychainStakingBaseInfo != null) {
            StakingDashboardItemLocal.staking(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                stake = relaychainStakingBaseInfo.stakingLedger.active,
                status = secondaryInfo?.status ?: fromCache?.status,
                rewards = secondaryInfo?.rewards ?: fromCache?.rewards,
                estimatedEarnings = secondaryInfo?.estimatedEarnings ?: fromCache?.estimatedEarnings,
                stakeStatusAccount = relaychainStakingBaseInfo.stakingLedger.stashId,
                rewardsAccount = relaychainStakingBaseInfo.stakingLedger.stashId,
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
        baseInfo: RelaychainStakingBaseInfo?,
        activeEra: EraIndex,
        multiChainStakingStats: MultiChainStakingStats,
    ): RelaychainStakingSecondaryInfo? {
        val chainStakingStats = multiChainStakingStats[stakingOptionId()] ?: return null

        return RelaychainStakingSecondaryInfo(
            rewards = chainStakingStats.rewards,
            estimatedEarnings = chainStakingStats.estimatedEarnings.value,
            status = determineStakingStatus(baseInfo, activeEra, chainStakingStats)
        )
    }

    private fun determineStakingStatus(
        baseInfo: RelaychainStakingBaseInfo?,
        activeEra: EraIndex,
        chainStakingStats: ChainStakingStats,
    ): Status? {
        return when {
            baseInfo == null -> null
            baseInfo.stakingLedger.active.isZero -> Status.INACTIVE
            baseInfo.nominations == null && baseInfo.validatorPrefs == null -> Status.INACTIVE
            chainStakingStats.accountPresentInActiveStakers -> Status.ACTIVE
            baseInfo.nominations != null && baseInfo.nominations.isWaiting(activeEra) -> Status.WAITING
            else -> Status.INACTIVE
        }
    }
}

private class RelaychainStakingBaseInfo(
    val stakingLedger: StakingLedger,
    val nominations: Nominations?,
    val validatorPrefs: ValidatorPrefs?,
)

private class RelaychainStakingSecondaryInfo(
    val rewards: Balance,
    val estimatedEarnings: Double,
    val status: Status?
)
