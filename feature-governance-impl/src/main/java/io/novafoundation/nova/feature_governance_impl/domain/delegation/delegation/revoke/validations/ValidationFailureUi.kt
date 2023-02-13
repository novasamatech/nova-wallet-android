package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError

fun handleRevokeDelegationValidationFailure(failure: RevokeDelegationValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (failure) {
        is RevokeDelegationValidationFailure.NotEnoughToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
    }
}
