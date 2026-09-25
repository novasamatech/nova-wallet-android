package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import android.util.Log
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionSource
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionUpdates
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.conflate
import java.math.BigInteger

class StakingDashboardBittensorUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    metaAccount: MetaAccount,
    private val positionSource: BittensorPositionSource,
    private val cache: StakingDashboardCache,
    private val positionUpdates: BittensorPositionUpdates,
    private val swapSource: BittensorSwapSource
) : BaseStakingDashboardUpdater(chain, chainAsset, Chain.Asset.StakingType.BITTENSOR_ROOT, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> = flow {
        val coldkey = metaAccount.accountIdIn(chain)
        if (coldkey == null) {
            savePosition(BigInteger.ZERO, false, null)
            emit(primarySynced())
            return@flow
        }

        val periodicRefreshes = flow {
            while (true) {
                delay(REFRESH_INTERVAL_MILLIS)
                emit(Unit)
            }
        }
        merge(periodicRefreshes, positionUpdates.updates.map { Unit }).conflate().collect {
            try {
                val root = positionSource.rootPositions(chain.id, coldkey)
                val subnets = positionSource.subnetPositions(chain.id, coldkey)
                val prices = subnets.map { it.subnetId }.distinct().mapNotNull { subnetId ->
                    try {
                        subnetId to swapSource.currentAlphaPrice(chain.id, subnetId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("BittensorDashboard", "Failed to value subnet ${subnetId.value}", e)
                        null
                    }
                }.toMap()
                val positions = BittensorPortfolioPositions(root, subnets, prices)
                savePosition(positions.totalStake, root.isNotEmpty() || subnets.isNotEmpty(), coldkey)
                emit(primarySynced())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("BittensorDashboard", "Failed to refresh staking positions", e)
            }
        }
    }

    private suspend fun savePosition(stake: BigInteger?, hasStake: Boolean, coldkey: ByteArray?) = cache.update { previous ->
        if (hasStake && coldkey != null) {
            StakingDashboardItemLocal(
                chainId = chain.id,
                chainAssetId = chainAsset.id,
                stakingType = stakingTypeLocal,
                metaId = metaAccount.id,
                hasStake = true,
                stake = stake,
                stakeStatusAccount = coldkey,
                rewardsAccount = coldkey,
                status = previous?.status,
                rewards = previous?.rewards,
                estimatedEarnings = previous?.estimatedEarnings
            )
        } else {
            StakingDashboardItemLocal.notStaking(chain.id, chainAsset.id, stakingTypeLocal, metaAccount.id, previous?.estimatedEarnings)
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 30_000L
    }
}
