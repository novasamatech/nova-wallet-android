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
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.TransferMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
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

            // Nova service fee (subnet only, root is exempt). Inert when the
            // recipient is unset or the floored fee rounds to 0. When charged,
            // we prepend a transferKeepAlive leg and reduce the staked amount by
            // the fee; the two .call()s auto-wrap in utility.batchAll (default
            // BatchMode.BATCH_ALL) so both land atomically.
            val novaFeeRecipient = SubtensorStakingConstants.NOVA_FEE_RECIPIENT
            val novaFee = if (netuid != SubtensorStakingConstants.ROOT_NETUID && novaFeeRecipient != null) {
                SubtensorStakingConstants.novaFeeAmount(amountInPlanks)
            } else {
                BigInteger.ZERO
            }

            extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                if (novaFeeRecipient != null && novaFee > BigInteger.ZERO) {
                    nativeTransfer(novaFeeRecipient, novaFee, TransferMode.KEEP_ALIVE)
                }

                addStakeLimit(
                    hotkey = hotkey,
                    netuid = netuid,
                    amount = amountInPlanks - novaFee,
                    spotPriceTaoPerAlpha = spotPrice,
                )
            }.awaitInBlock().getOrThrow()

            positionCache.invalidate(coldkey)
        }
    }

    /**
     * Submits `SubtensorModule.remove_stake_limit`. The caller passes the
     * [spotPriceTaoPerAlpha] it already resolved (and displayed on the confirm
     * screen), so the limit_price cushion AND the Nova fee are derived from the
     * SAME reserve read the user saw — no second fetch, no displayed-vs-charged
     * drift. Mirrors iOS `SubtensorUnstakeConfirmInteractor`, which computes the
     * commission once and reuses it for both the row and the extrinsic. `null`
     * (root, or an unresolved estimate) means no Nova fee and a conservative
     * fallback limit_price.
     */
    suspend fun submitUnstake(
        netuid: Int,
        hotkey: ByteArray,
        amountInPlanks: BigInteger,
        spotPriceTaoPerAlpha: Double?,
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            val chain = stakingSharedState.chain()
            val metaAccount = accountRepository.getSelectedMetaAccount()
            val coldkey = metaAccount.requireAccountIdIn(chain)

            // Nova service fee (subnet only, root is exempt). Based on the
            // *minimum* TAO the unstake can return — alpha * spotPrice cushioned
            // by DEFAULT_SLIPPAGE — computed from the SAME spot price the screen
            // displayed, so charged == shown. Inert when the recipient is unset,
            // the spot price is unresolved, or the floored fee rounds to 0.
            val novaFeeRecipient = SubtensorStakingConstants.NOVA_FEE_RECIPIENT
            val novaFee = if (
                netuid != SubtensorStakingConstants.ROOT_NETUID &&
                novaFeeRecipient != null &&
                spotPriceTaoPerAlpha != null &&
                spotPriceTaoPerAlpha > 0.0
            ) {
                val minTaoOut = (
                    amountInPlanks.toBigDecimal() *
                        spotPriceTaoPerAlpha.toBigDecimal() *
                        (1.0 - SubtensorStakingConstants.DEFAULT_SLIPPAGE).toBigDecimal()
                    ).toBigInteger()
                SubtensorStakingConstants.novaFeeAmount(minTaoOut)
            } else {
                BigInteger.ZERO
            }

            // Fee leg comes LAST here: unstake settles TAO into the coldkey
            // first, then the fee is swept out. The two .call()s auto-wrap in
            // utility.batchAll (default BatchMode.BATCH_ALL) so both are atomic.
            extrinsicService.submitAndWatchExtrinsic(chain, TransactionOrigin.SelectedWallet) {
                removeStakeLimit(
                    hotkey = hotkey,
                    netuid = netuid,
                    amount = amountInPlanks,
                    spotPriceTaoPerAlpha = spotPriceTaoPerAlpha,
                )

                if (novaFeeRecipient != null && novaFee > BigInteger.ZERO) {
                    nativeTransfer(novaFeeRecipient, novaFee, TransferMode.KEEP_ALIVE)
                }
            }.awaitInBlock().getOrThrow()

            positionCache.invalidate(coldkey)
        }
    }
}
