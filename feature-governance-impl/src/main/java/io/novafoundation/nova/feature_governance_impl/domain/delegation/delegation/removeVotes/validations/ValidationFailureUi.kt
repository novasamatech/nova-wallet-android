package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError

fun handleRemoveVotesValidationFailure(failure: RemoveVotesValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (failure) {
        is RemoveVotesValidationFailure.NotEnoughToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
    }
}
