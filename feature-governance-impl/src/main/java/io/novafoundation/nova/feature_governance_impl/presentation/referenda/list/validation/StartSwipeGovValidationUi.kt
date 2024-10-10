package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.validation

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.domain.validation.handleChainAccountNotFound
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.validation.StartSwipeGovValidationFailure

fun handleStartSwipeGovValidationFailure(
    resourceManager: ResourceManager,
    validationStatus: ValidationStatus.NotValid<StartSwipeGovValidationFailure>
): TransformedFailure {
    return when (val reason = validationStatus.reason) {
        is StartSwipeGovValidationFailure.NoChainAccountFound -> handleChainAccountNotFound(
            failure = reason,
            addAccountDescriptionRes = R.string.common_network_not_supported,
            resourceManager = resourceManager,
            goToWalletDetails = { }
        )
    }
}
