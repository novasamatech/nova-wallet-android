package io.novafoundation.nova.feature_staking_impl.domain.staking.rewardDestination

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.controllerTransactionOrigin
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.calls.setPayee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChangeRewardDestinationInteractor(
    private val extrinsicService: ExtrinsicService
) {

    suspend fun estimateFee(
        stashState: StakingState.Stash,
        rewardDestination: RewardDestination,
    ): Fee = withContext(Dispatchers.IO) {
        extrinsicService.estimateFee(stashState.chain, stashState.controllerTransactionOrigin()) {
            setPayee(rewardDestination)
        }
    }

    suspend fun changeRewardDestination(
        stashState: StakingState.Stash,
        rewardDestination: RewardDestination,
    ): Result<ExtrinsicSubmission> = withContext(Dispatchers.IO) {
        extrinsicService.submitExtrinsic(stashState.chain, stashState.controllerTransactionOrigin()) {
            setPayee(rewardDestination)
        }
    }
}
