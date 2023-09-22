package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.claimRewards.validations.NominationPoolsClaimRewardsValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.validation.handleInsufficientBalanceCommission

fun nominationPoolsClaimRewardsValidationFailure(
    failure: NominationPoolsClaimRewardsValidationFailure,
    resourceManager: ResourceManager
): TitleAndMessage {
    return when (failure) {
        is NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(failure, resourceManager)

        NominationPoolsClaimRewardsValidationFailure.NonProfitableClaim -> resourceManager.getString(R.string.common_confirmation_title) to
            resourceManager.getString(R.string.staking_warning_tiny_payout)

        is NominationPoolsClaimRewardsValidationFailure.ToStayAboveED -> handleInsufficientBalanceCommission(
            failure.asset,
            resourceManager
        )
    }
}
