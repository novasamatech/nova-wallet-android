package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.redeem.validations.NominationPoolsRedeemValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun nominationPoolsRedeemValidationFailure(
    failure: NominationPoolsRedeemValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (failure) {
        is NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
        is NominationPoolsRedeemValidationFailure.ToStayAboveED -> handleInsufficientBalanceCommission(
            failure.asset,
            resourceManager
        )
    }
}
