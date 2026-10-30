package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorDashboardSnapshotRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPositionRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimSource
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class StakingDashboardBittensorUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    metaAccount: MetaAccount,
    private val positionRepository: BittensorPositionRepository,
    private val cache: StakingDashboardCache,
    private val rootClaims: BittensorRootClaimSource,
    private val snapshots: BittensorDashboardSnapshotRepository,
    private val logger: Logger
) : BaseStakingDashboardUpdater(chain, chainAsset, Chain.Asset.StakingType.BITTENSOR_ROOT, metaAccount) {

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect> {
        val coldKey = metaAccount.accountIdIn(chain)
        if (coldKey == null) {
            return flow {
                savePosition(BigInteger.ZERO, false, null)
                emit(primarySynced())
            }
        }

        return flow {
            // Like iOS byKeepingRootRedeemable: a failed read reuses the last known claimable amount,
            // never the stored classification, which may predate a full exit.
            var lastClaimable: BigInteger? = null

            positionRepository.observePortfolio(chain.id, coldKey).collect { state ->
                val positions = state.positions
                if (state.status !is BittensorDataStatus.Fresh || positions == null) return@collect

                // Rewards are read with every stake state; next to a subnet stake they add Root to the composition.
                val staked = positions.root.isNotEmpty() || positions.subnets.isNotEmpty()
                if (staked) savePosition(positions.totalStake, true, coldKey)

                val claimable = claimableRootRewards(coldKey)
                    ?: lastClaimable
                    ?: snapshots.claimableRootRewards(chain.id, coldKey)
                    ?: BigInteger.ZERO
                lastClaimable = claimable
                snapshots.saveClaimableRootRewards(chain.id, coldKey, claimable)
                if (!staked) savePosition(positions.totalStake, claimable.isPositive(), coldKey)

                emit(primarySynced())
            }
        }
    }

    /** Only rewards meeting the claim minimum count, as in iOS hasActiveStaking. Null when unavailable. */
    private suspend fun claimableRootRewards(coldKey: AccountId): BigInteger? = try {
        rootClaims.claimPreviews(chain.id, coldKey).totalClaimable
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        logRewardsFailure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logRewardsFailure(e)
    }

    private fun logRewardsFailure(cause: Throwable): BigInteger? {
        logger.log("[BittensorDashboard] Root rewards unavailable; using the last known claimable amount")
        logger.log(cause)
        return null
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
