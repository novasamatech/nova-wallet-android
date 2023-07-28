@file:OptIn(ExperimentalCoroutinesApi::class)

package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.common.data.network.runtime.binding.bindNullableAccountId
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal.Status
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_api.domain.model.Nominations
import io.novafoundation.nova.feature_staking_api.domain.model.StakingLedger
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.ChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindActiveEra
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindNominationsOrNull
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings.bindStakingLedgerOrNull
import io.novafoundation.nova.feature_staking_impl.domain.common.isWaiting
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.metadata
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
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
    private val stakingStatsFlow: Flow<IndexedValue<MultiChainStakingStats>>,
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        val accountId = metaAccount.accountIdIn(chain)

        return remoteStorageSource.subscribe(chain.id, storageSubscriptionBuilder) {
            val activeEraFlow = metadata.staking().storage("ActiveEra").observe(binding = ::bindActiveEra)

            val baseInfo = if (accountId != null) {
                val bondedFlow = metadata.staking().storage("Bonded").observe(accountId, binding = ::bindNullableAccountId)

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
            metadata.staking().storage("Ledger").observe(controllerId, binding = ::bindStakingLedgerOrNull).flatMapLatest { ledger ->
                if (ledger != null) {
                    subscribeToNominations(ledger.stashId).map { nominations ->
                        RelaychainStakingBaseInfo(ledger, nominations)
                    }
                } else {
                    flowOf(null)
                }
            }
        }
    }

    private suspend fun subscribeToNominations(stashId: AccountId): Flow<Nominations?> {
        return remoteStorageSource.subscribe(chain.id) {
            metadata.staking().storage("Nominators").observe(stashId, binding = ::bindNominationsOrNull)
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
                primaryStakingAccountId = relaychainStakingBaseInfo.stakingLedger.stashId,
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
            baseInfo.nominations == null -> Status.INACTIVE
            chainStakingStats.accountPresentInActiveStakers -> Status.ACTIVE
            baseInfo.nominations.isWaiting(activeEra) -> Status.WAITING
            else -> Status.INACTIVE
        }
    }
}

private class RelaychainStakingBaseInfo(
    val stakingLedger: StakingLedger,
    val nominations: Nominations?,
)

private class RelaychainStakingSecondaryInfo(
    val rewards: Balance,
    val estimatedEarnings: Double,
    val status: Status?
)
