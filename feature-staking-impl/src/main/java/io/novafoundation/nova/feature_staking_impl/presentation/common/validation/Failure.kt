package io.novafoundation.nova.feature_staking_impl.presentation.common.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.NotEnoughStakeable
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.CannotPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.MaxNominatorsReached
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.AmountLessThanMinimum
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.NotEnoughFundToStayAboveED
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.handleStakingMinimumBondError
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun stakingValidationFailure(
    reason: SetupStakingValidationFailure,
    resourceManager: ResourceManager,
): TitleAndMessage {
    val (title, message) = with(resourceManager) {
        when (reason) {
            NotEnoughStakeable, CannotPayFee -> amountIsTooBig()

            MaxNominatorsReached -> {
                getString(R.string.staking_max_nominators_reached_title) to getString(R.string.staking_max_nominators_reached_message)
            }

            is AmountLessThanMinimum -> handleStakingMinimumBondError(resourceManager, reason)

            is NotEnoughFundToStayAboveED -> handleInsufficientBalanceCommission(
                reason,
                resourceManager
            )
        }
    }

    return title to message
}

fun unbondPreliminaryValidationFailure(
    reason: ParachainStakingUnbondPreliminaryValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (reason) {
        ParachainStakingUnbondPreliminaryValidationFailure.NoAvailableCollators -> {
            resourceManager.getString(R.string.staking_parachain_no_unbond_collators_title) to
                resourceManager.getString(R.string.staking_parachain_no_unbond_collators_message)
        }
    }
}
