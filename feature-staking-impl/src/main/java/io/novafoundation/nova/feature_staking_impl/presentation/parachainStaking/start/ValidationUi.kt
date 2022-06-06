package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun startParachainStakingValidationFailure(
    failure: StartParachainStakingValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (failure) {
        NotEnoughBalanceToPayFees -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.common_not_enough_funds_message)
        }
        is TooLowStake -> {
            val formattedMinStake = mapAmountToAmountModel(failure.minimumStake, failure.asset).token

            when (failure) {
                is TooLowStake.TooLowDelegation -> {
                    val messageFormat = if (failure.strictGreaterThan) R.string.staking_setup_amount_too_low_strict else R.string.staking_setup_amount_too_low

                    resourceManager.getString(R.string.common_amount_low) to
                        resourceManager.getString(messageFormat, formattedMinStake)
                }
                is TooLowStake.TooLowTotalStake -> {

                    resourceManager.getString(R.string.common_amount_low) to
                        resourceManager.getString(R.string.staking_setup_amount_too_low, formattedMinStake)
                }
                is TooLowStake.WontReceiveRewards -> {
                    resourceManager.getString(R.string.staking_parachain_wont_receive_rewards_title) to
                        resourceManager.getString(R.string.staking_parachain_wont_receive_rewards_message, formattedMinStake)
                }
            }
        }
        StartParachainStakingValidationFailure.NotPositiveAmount -> {
            resourceManager.getString(R.string.common_amount_low) to
                resourceManager.getString(R.string.common_zero_amount_error)
        }

        StartParachainStakingValidationFailure.CollatorIsNotActive -> {
            resourceManager.getString(R.string.parachain_staking_not_active_collator_title) to
                resourceManager.getString(R.string.parachain_staking_not_active_collator_message)
        }
    }
}
