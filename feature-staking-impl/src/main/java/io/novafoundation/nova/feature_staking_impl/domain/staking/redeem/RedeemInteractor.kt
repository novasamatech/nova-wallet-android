package io.novafoundation.nova.feature_staking_impl.domain.staking.redeem

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.api.StakingRepository
import io.novafoundation.nova.feature_staking_api.domain.model.numberOfSlashingSpans
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.controllerTransactionOrigin
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.withdrawUnbonded
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

class RedeemInteractor(
    private val extrinsicService: ExtrinsicService,
    private val stakingRepository: StakingRepository,
) {

    suspend fun estimateFee(stakingState: StakingState.Stash): Fee {
        return withContext(Dispatchers.IO) {
            extrinsicService.estimateFee(stakingState.chain, stakingState.controllerTransactionOrigin()) {
                withdrawUnbonded(getSlashingSpansNumber(stakingState))
            }
        }
    }

    suspend fun redeem(stakingState: StakingState.Stash, asset: Asset): Result<Pair<ExtrinsicSubmission, RedeemConsequences>> {
        return withContext(Dispatchers.IO) {
            extrinsicService.submitExtrinsic(stakingState.chain, stakingState.controllerTransactionOrigin()) {
                withdrawUnbonded(getSlashingSpansNumber(stakingState))
            }.map {
                it to RedeemConsequences(willKillStash = asset.isRedeemingAll())
            }
        }
    }

    private fun Asset.isRedeemingAll(): Boolean {
        return bonded.isZero && unbonding.isZero
    }

    private suspend fun getSlashingSpansNumber(stakingState: StakingState.Stash): BigInteger {
        return stakingRepository.getSlashingSpan(stakingState.chain.id, stakingState.stashId).numberOfSlashingSpans()
    }
}
