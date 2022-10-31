package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError

fun handleUnlockReferendumValidationFailure(failure: UnlockGovernanceValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (failure) {
        is UnlockGovernanceValidationFailure.NotEnoughToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
    }
}
