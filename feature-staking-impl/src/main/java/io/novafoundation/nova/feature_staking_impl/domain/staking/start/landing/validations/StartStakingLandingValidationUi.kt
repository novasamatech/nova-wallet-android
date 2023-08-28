package io.novafoundation.nova.feature_staking_impl.domain.staking.start.landing.validations

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.domain.validation.handleChainAccountNotFound
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter

fun handleStartStakingLandingValidationFailure(
    resourceManager: ResourceManager,
    validationStatus: ValidationStatus.NotValid<StartStakingLandingValidationFailure>,
    router: StartMultiStakingRouter,
) : TransformedFailure {
    return when(val reason = validationStatus.reason) {
        is StartStakingLandingValidationFailure.NoChainAccountFound -> handleChainAccountNotFound(
            failure = reason,
            addAccountDescriptionRes = R.string.staking_missing_account_message,
            resourceManager = resourceManager,
            goToWalletDetails = router::goToWalletDetails
        )
    }
}
