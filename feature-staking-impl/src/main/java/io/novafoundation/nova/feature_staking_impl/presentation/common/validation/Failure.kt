package io.novafoundation.nova.feature_staking_impl.presentation.common.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.AmountLessThanAllowed
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.AmountLessThanRecommended
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.CannotPayFee
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.MaxNominatorsReached
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.SetupStakingValidationFailure.NotEnoughStakeable
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import java.math.RoundingMode

fun stakingValidationFailure(
    payload: SetupStakingPayload,
    reason: SetupStakingValidationFailure,
    resourceManager: ResourceManager,
): TitleAndMessage {
    val (title, message) = with(resourceManager) {
        when (reason) {
            NotEnoughStakeable, CannotPayFee -> {
                getString(R.string.common_amount_too_big) to getString(R.string.choose_amount_error_too_big)
            }

            is AmountLessThanAllowed -> {
                val formattedThreshold = reason.threshold.formatTokenAmount(payload.stashAsset.token.configuration, RoundingMode.UP)

                getString(R.string.common_amount_low) to getString(R.string.staking_setup_amount_too_low, formattedThreshold)
            }

            is AmountLessThanRecommended -> {
                val formattedThreshold = reason.threshold.formatTokenAmount(payload.stashAsset.token.configuration, RoundingMode.UP)

                getString(R.string.common_amount_low) to getString(R.string.staking_setup_amount_less_than_recommended, formattedThreshold)
            }

            MaxNominatorsReached -> {
                getString(R.string.staking_max_nominators_reached_title) to getString(R.string.staking_max_nominators_reached_message)
            }
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
