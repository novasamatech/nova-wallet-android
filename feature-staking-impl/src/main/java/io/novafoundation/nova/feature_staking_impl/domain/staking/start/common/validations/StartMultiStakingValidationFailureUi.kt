package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.AmountLessThanMinimum
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.MaxNominatorsReached
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.NotEnoughAvailableToStake
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationFailure.NotEnoughToPayFees
import io.novafoundation.nova.feature_staking_impl.domain.validations.setup.handleStakingMinimumBondError
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.formatStakingTypeLabel
import io.novafoundation.nova.feature_wallet_api.domain.validation.amountIsTooBig
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.zeroAmount

fun handleStartMultiStakingValidationFailure(error: StartMultiStakingValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when(error) {
        is AmountLessThanMinimum -> handleStakingMinimumBondError(resourceManager, error)

        is NotEnoughToPayFees -> handleNotEnoughFeeError(error, resourceManager)

        is MaxNominatorsReached -> {
            val stakingTypeLabel = resourceManager.formatStakingTypeLabel(error.stakingType)

            resourceManager.getString(R.string.start_staking_max_nominators_reached_title, stakingTypeLabel) to
                resourceManager.getString(R.string.start_staking_max_nominators_reached_message)
        }

        NotEnoughAvailableToStake -> resourceManager.amountIsTooBig()

        StartMultiStakingValidationFailure.NonPositiveAmount -> resourceManager.zeroAmount()
    }
}
