package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform

class StakingDashboardBittensorUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    metaAccount: MetaAccount,
    private val positionRepository: BittensorPositionRepository,
    private val cache: StakingDashboardCache
) : BaseStakingDashboardUpdater(chain, chainAsset, Chain.Asset.StakingType.BITTENSOR_ROOT, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        val coldKey = metaAccount.accountIdIn(chain)
        if (coldKey == null) {
            return flow {
                savePosition(BigInteger.ZERO, false, null)
                emit(primarySynced())
            }
        }

        return positionRepository.observePortfolio(chain.id, coldKey)
            .transform { state ->
                val positions = state.positions
                if (state.status is BittensorDataStatus.Fresh && positions != null) {
                    savePosition(positions.totalStake, positions.root.isNotEmpty() || positions.subnets.isNotEmpty(), coldKey)
                    emit(primarySynced())
                }
            }
    }

    private suspend fun savePosition(stake: BigInteger?, hasStake: Boolean, coldKey: AccountId?) = cache.update { previous ->
        if (hasStake && coldKey != null) {
            StakingDashboardItemLocal(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                hasStake = true,
                stake = stake,
                stakeStatusAccount = coldKey,
                rewardsAccount = coldKey,
                status = previous?.status,
                rewards = previous?.rewards,
                estimatedEarnings = previous?.estimatedEarnings
            )
        } else {
            StakingDashboardItemLocal.notStaking(chain.id, chainAsset.id, stakingTypeLocal, metaAccount.id, previous?.estimatedEarnings)
        }
    }
}
