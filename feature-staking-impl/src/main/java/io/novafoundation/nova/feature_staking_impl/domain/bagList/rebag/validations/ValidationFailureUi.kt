package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError

fun handleRebagValidationFailure(failure: RebagValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (failure) {
        is RebagValidationFailure.NotEnoughToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
    }
}
