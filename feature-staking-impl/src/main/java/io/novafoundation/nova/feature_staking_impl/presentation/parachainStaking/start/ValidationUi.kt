package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.CollatorIsNotActive
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.NotEnoughStakeableBalance
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.NotPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.PendingRevoke
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations.StartParachainStakingValidationFailure.TooLowStake
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
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

        NotEnoughStakeableBalance -> resourceManager.amountIsTooBig()

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
        NotPositiveAmount -> resourceManager.zeroAmount()

        CollatorIsNotActive -> {
            resourceManager.getString(R.string.parachain_staking_cannot_stake_with_collator) to
                resourceManager.getString(R.string.parachain_staking_not_active_collator_message)
        }
        PendingRevoke -> {
            resourceManager.getString(R.string.parachain_staking_collator_cannot_bond_more) to
                resourceManager.getString(R.string.parachain_staking_pending_revoke_message)
        }
    }
}
