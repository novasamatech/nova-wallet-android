package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import android.util.Log
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.MultiChainOffChainSyncResult
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.math.BigInteger
import kotlin.time.Duration.Companion.seconds

/**
 * Single dashboard updater for Bittensor (TAO) staking.
 *
 * The framework only ever calls `createUpdater(chain, stakingType)` once per
 * (chain, stakingType) tuple — it iterates `chain.utilityAsset.supportedStakingOptions()`
 * for the fan-out. So we get one instance for SUBTENSOR per chain.
 *
 * To match iOS — which renders one dashboard row per (TAO root + 128 subnet
 * alpha) — this single updater walks every chain asset that declares
 * `staking: ["subtensor"]` and writes its own dashboard item per cycle.
 * Each (chainId, assetId, SUBTENSOR) tuple is treated as a separate sync
 * option so the framework can track sync state per row.
 *
 * All instances share the per-coldkey [SubtensorPositionCache] so the
 * runtime API only fires once per polling tick regardless of how many
 * assets we update.
 *
 * Mirrors `SubtensorMultistakingUpdateService.swift` on iOS.
 */
class StakingDashboardSubtensorUpdater(
    chain: Chain,
    chainAsset: Chain.Asset,
    stakingType: Chain.Asset.StakingType,
    metaAccount: MetaAccount,
    private val positionCache: SubtensorPositionCache,
    private val stakingDashboardCache: StakingDashboardCache,
    @Suppress("UnusedPrivateProperty")
    private val stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
) : BaseStakingDashboardUpdater(chain, chainAsset, stakingType, metaAccount) {

    /** Pre-computed per cycle: every asset on this chain that supports SUBTENSOR. */
    private val subtensorAssets: List<Chain.Asset> = chain.assets.filter { stakingType in it.staking }

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
    ): Flow<Updater.SideEffect> = pollPositions()

    private fun pollPositions(): Flow<Updater.SideEffect> = flow {
        val coldkey = metaAccount.accountIdIn(chain) ?: run {
            Log.w(TAG, "no coldkey for ${chain.name}; updater not polling")
            return@flow
        }
        Log.d(
            TAG,
            "starting poll for ${chain.name} across ${subtensorAssets.size} assets " +
                "(coldkey=${coldkey.take(4).joinToString("") { "%02x".format(it) }}…)",
        )

        while (true) {
            try {
                val positions = positionCache.positions(chain.id, coldkey)
                Log.d(
                    TAG,
                    "fetched ${positions.size} positions: " +
                        positions.joinToString { "(netuid=${it.netuid}, amount=${it.amount})" },
                )

                // For each subtensor-staking asset on this chain, scope the
                // position list to that asset's netuid and persist a row.
                // The dashboard interactor filters out empty alpha rows from
                // "Available to stake" so users with stake on only a few
                // subnets don't see 128 zero rows.
                subtensorAssets.forEach { asset ->
                    val assetNetuid = asset.subtensorNetuid()
                    val totalForAsset = positions
                        .asSequence()
                        .filter { it.netuid == assetNetuid }
                        .fold(BigInteger.ZERO) { acc, p -> acc + p.amount }

                    saveDashboardItemFor(asset, coldkey, totalForAsset)

                    val optionId = StakingOptionId(chain.id, asset.id, stakingType)
                    emit(StakingDashboardUpdaterEvent.PrimarySynced(optionId))
                    emit(StakingDashboardUpdaterEvent.AllSynced(optionId, NO_OFF_CHAIN_INDEX))
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Re-throw so the framework can cancel us cleanly.
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "fetch failed: ${e.javaClass.simpleName}: ${e.message}")
                // Mark every row synced anyway so indicators don't stick.
                subtensorAssets.forEach { asset ->
                    val optionId = StakingOptionId(chain.id, asset.id, stakingType)
                    emit(StakingDashboardUpdaterEvent.PrimarySynced(optionId))
                    emit(StakingDashboardUpdaterEvent.AllSynced(optionId, NO_OFF_CHAIN_INDEX))
                }
            }
            kotlinx.coroutines.delay(SubtensorStakingConstants.DASHBOARD_RESYNC_SECONDS.seconds)
        }
    }

    private suspend fun saveDashboardItemFor(asset: Chain.Asset, coldkey: ByteArray, totalStake: BigInteger) {
        stakingDashboardCache.update(chain.id, asset.id, stakingTypeLocal, metaAccount.id) { _ ->
            if (totalStake.signum() > 0) {
                StakingDashboardItemLocal.staking(
                    chainId = chain.id,
                    chainAssetId = asset.id,
                    stakingType = stakingTypeLocal,
                    metaId = metaAccount.id,
                    stake = totalStake,
                    status = StakingDashboardItemLocal.Status.ACTIVE,
                    // Mirrors iOS `StakingDashboardSubtensorMapper.swift:34-41`:
                    // the storage layer leaves rewards / estimatedEarnings nil
                    // for Subtensor — iOS and Android both lack an offchain
                    // indexer, so neither value is meaningful at write time.
                    // The DEBUG-only [TEMP-TAOSTATS] 0.18 APY fallback lives at
                    // the read layer (StakingDashboardRepository.hasStakeState)
                    // so release builds match iOS's "leave it nil" semantics.
                    rewards = null,
                    estimatedEarnings = null,
                    stakeStatusAccount = coldkey,
                    rewardsAccount = coldkey,
                )
            } else {
                StakingDashboardItemLocal.notStaking(
                    chainId = chain.id,
                    chainAssetId = asset.id,
                    stakingType = stakingTypeLocal,
                    metaId = metaAccount.id,
                    estimatedEarnings = null,
                )
            }
        }
    }

    companion object {
        private const val TAG = "StakingDashboardSubtensor"

        // Subtensor has no SubQuery indexer; pass MAX_VALUE so the
        // `event.indexOfUsedOffChainSync >= latestOffChainSyncIndex.value`
        // gate in `RealStakingDashboardUpdateSystem.handleUpdaterEvent`
        // always advances to SYNCED.
        private const val NO_OFF_CHAIN_INDEX = Int.MAX_VALUE

    }
}

/**
 * Resolves the netuid an asset corresponds to.
 *
 *  - Native (utility) asset = root TAO, netuid 0.
 *  - SubtensorAlpha = the netuid embedded in the type.
 *  - Anything else (no nova-utils mapping yet) falls back to a "SN<N>"
 *    symbol regex so older configs still resolve.
 */
internal fun Chain.Asset.subtensorNetuid(): Int = when (val type = type) {
    is Chain.Asset.Type.SubtensorAlpha -> type.netuid
    Chain.Asset.Type.Native -> SubtensorStakingConstants.ROOT_NETUID
    else -> Regex("^SN(\\d+)$").find(symbol.value)
        ?.groupValues?.getOrNull(1)?.toIntOrNull()
        ?: SubtensorStakingConstants.ROOT_NETUID
}
