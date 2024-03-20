package io.novafoundation.nova.feature_staking_impl.data.mappers

import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAccountId
import io.novafoundation.nova.feature_staking_api.domain.model.RewardDestination
import io.novafoundation.nova.feature_staking_impl.presentation.common.rewardDestination.RewardDestinationModel

fun mapRewardDestinationModelToRewardDestination(
    rewardDestinationModel: RewardDestinationModel,
): RewardDestination {
    return when (rewardDestinationModel) {
        is RewardDestinationModel.Restake -> RewardDestination.Restake
        is RewardDestinationModel.Payout -> RewardDestination.Payout(rewardDestinationModel.destination.address.toAccountId())
    }
}
