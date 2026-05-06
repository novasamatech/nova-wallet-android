package io.novafoundation.nova.feature_staking_impl.domain.subtensor

import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.subtensor.extrinsic.addStakeLimit
import io.novafoundation.nova.feature_staking_impl.data.subtensor.extrinsic.removeStakeLimit
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorPositionCache
import io.novafoundation.nova.feature_staking_impl.data.subtensor.network.SubtensorSubnetFetcher
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

/**
 * Submits a Bittensor `add_stake_limit` extrinsic and waits for it to land
 * in a block. Mirrors iOS `SubtensorStakeConfirmInteractor.doConfirmExtrinsic`.
 *
 * For subnet flows the caller-supplied netuid drives a live AMM-reserve
 * fetch so the limit_price cushion reflects current chain state. Root flows
 * skip the reserve fetch entirely (no AMM).
 *
 * On `inBlock` the per-coldkey [SubtensorPositionCache] is invalidated so
 * the next dashboard sync sees the new position rather than the pre-stake
 * snapshot.
 */
class SubtensorStakeSubmitInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingSharedState: StakingSharedState,
    private val accountRepository: AccountRepository,
    private val subnetFetcher: SubtensorSubnetFetcher,
    private val positionCache: SubtensorPositionCache,
) {

    suspend fun submitStake(
        netuid: Int,
        hotkey: ByteArray,
        amountInPlanks: BigInteger,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val chain = stakingSharedState.chain()
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val coldkey = metaAccount.requireAccountIdIn(chain)

            val spotPrice: Double? = if (netuid != SubtensorStakingConstants.ROOT_NETUID) {
                runCatching { subnetFetcher.fetchReserves(netuid) }.getOrNull()?.spotPrice
            } else {
                null
            }

            extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                addStakeLimit(
                    hotkey = hotkey,
                    netuid = netuid,
                    amount = amountInPlanks,
                    spotPriceTaoPerAlpha = spotPrice,
                )
            }.awaitInBlock().getOrThrow()

            positionCache.invalidate(coldkey)
        }
    }

    /**
     * Submits `SubtensorModule.remove_stake_limit`. For subnet positions
     * (netuid != 0) we fetch live AMM reserves so the limit_price cushion
     * tracks the current chain state; root positions skip the fetch since
     * there is no AMM. Mirrors iOS `SubtensorUnstakeConfirmInteractor`.
     */
    suspend fun submitUnstake(
        netuid: Int,
        hotkey: ByteArray,
        amountInPlanks: BigInteger,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val chain = stakingSharedState.chain()
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val coldkey = metaAccount.requireAccountIdIn(chain)

            val spotPrice: Double? = if (netuid != SubtensorStakingConstants.ROOT_NETUID) {
                runCatching { subnetFetcher.fetchReserves(netuid) }.getOrNull()?.spotPrice
            } else {
                null
            }

            extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                removeStakeLimit(
                    hotkey = hotkey,
                    netuid = netuid,
                    amount = amountInPlanks,
                    spotPriceTaoPerAlpha = spotPrice,
                )
            }.awaitInBlock().getOrThrow()

            positionCache.invalidate(coldkey)
        }
    }
}
