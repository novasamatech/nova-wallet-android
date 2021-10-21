package io.novafoundation.nova.feature_staking_impl.domain.staking.redeem

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.withdrawUnbonded
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class RedeemInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingRepository: StakingRepository,
) {

    suspend fun estimateFee(stakingState: StakingState.Stash): BigInteger {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingState.chain) {
                withdrawUnbonded(getSlashingSpansNumber(stakingState))
            }
        }
    }

    suspend fun redeem(stakingState: StakingState.Stash, asset: Asset): Result<RedeemConsequences> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stakingState.chain, stakingState.controllerId) {
                withdrawUnbonded(getSlashingSpansNumber(stakingState))
            }.map {
                RedeemConsequences(
                    willKillStash = asset.redeemable == asset.locked
                )
            }
        }
    }

    private suspend fun getSlashingSpansNumber(stakingState: StakingState.Stash): BigInteger {
        val slashingSpans = stakingRepository.getSlashingSpan(stakingState.chain.id, stakingState.stashId)

        return slashingSpans?.let {
            val totalSpans = it.prior.size + 1 //  all from prior + one for lastNonZeroSlash

            totalSpans.toBigInteger()
        } ?: BigInteger.ZERO
    }
}
