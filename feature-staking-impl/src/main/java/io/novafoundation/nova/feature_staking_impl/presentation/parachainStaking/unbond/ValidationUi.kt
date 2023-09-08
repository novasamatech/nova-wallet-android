package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.unbond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.AlreadyHasDelegationRequestToCollator
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.NotEnoughBondedToUnbond
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.NotPositiveAmount
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationFailure.TooLowRemainingBond
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow.ParachainStakingUnbondValidationPayload
import io.novafoundation.nova.feature_wallet_api.domain.validation.notSufficientBalanceToPayFeeErrorMessage
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun parachainStakingUnbondValidationFailure(
    failure: ParachainStakingUnbondValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (failure) {
        NotEnoughBalanceToPayFees -> resourceManager.notSufficientBalanceToPayFeeErrorMessage()

        is TooLowRemainingBond -> {
            val minimumRequired = mapAmountToAmountModel(failure.minimumRequired, failure.asset).token

            when (failure) {
                is TooLowRemainingBond.WontReceiveRewards -> {
                    resourceManager.getString(R.string.staking_parachain_wont_receive_rewards_title) to
                        resourceManager.getString(R.string.parachain_staking_unbond_no_rewards, minimumRequired)
                }
                is TooLowRemainingBond.WillBeAddedToUnbondings -> {
                    resourceManager.getString(R.string.staking_unstake_all_question) to
                        resourceManager.getString(R.string.parachain_staking_unstake_all, minimumRequired)
                }
            }
        }

        NotPositiveAmount -> resourceManager.zeroAmount()

        AlreadyHasDelegationRequestToCollator -> {
            resourceManager.getString(R.string.staking_parachain_unbond_already_exists_title) to
                resourceManager.getString(R.string.staking_parachain_unbond_already_exists_message)
        }

        NotEnoughBondedToUnbond -> {
            resourceManager.getString(R.string.common_not_enough_funds_title) to
                resourceManager.getString(R.string.staking_unbond_too_big)
        }
    }
}

fun parachainStakingUnbondPayloadAutoFix(payload: ParachainStakingUnbondValidationPayload, reason: ParachainStakingUnbondValidationFailure) = when (reason) {
    is TooLowRemainingBond.WillBeAddedToUnbondings -> payload.copy(amount = reason.newAmount)
    else -> payload
}
