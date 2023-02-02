package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.create.chooseAmount.validation

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFreeBalanceError

fun chooseChooseDelegationAmountValidationFailure(
    failure: ChooseDelegationAmountValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (failure) {
        is ChooseDelegationAmountValidationFailure.NotEnoughToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
        is ChooseDelegationAmountValidationFailure.AmountIsTooBig -> handleNotEnoughFreeBalanceError(
            error = failure,
            resourceManager = resourceManager,
            descriptionFormat = R.string.refrendum_vote_not_enough_available_message
        )
        ChooseDelegationAmountValidationFailure.CannotDelegateToSelf -> {
            resourceManager.getString(R.string.delegation_error_self_delegate_title) to
                resourceManager.getString(R.string.delegation_error_self_delegate_message)
        }
    }
}
