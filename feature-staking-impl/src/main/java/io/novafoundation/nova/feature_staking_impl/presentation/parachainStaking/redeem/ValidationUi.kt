package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations.ParachainStakingRedeemValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations.ParachainStakingRedeemValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_wallet_api.domain.validation.notSufficientBalanceToPayFeeErrorMessage

fun parachainStakingRedeemValidationFailure(reason: ParachainStakingRedeemValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (reason) {
        NotEnoughBalanceToPayFees -> resourceManager.notSufficientBalanceToPayFeeErrorMessage()
    }
}
