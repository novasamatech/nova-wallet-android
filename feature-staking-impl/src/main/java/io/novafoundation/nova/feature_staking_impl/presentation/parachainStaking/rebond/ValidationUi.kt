package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.rebond

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations.ParachainStakingRebondValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations.ParachainStakingRebondValidationFailure.NotEnoughBalanceToPayFees
import io.novafoundation.nova.feature_wallet_api.domain.validation.notSufficientBalanceToPayFeeErrorMessage

fun parachainStakingRebondValidationFailure(reason: ParachainStakingRebondValidationFailure, resourceManager: ResourceManager): TitleAndMessage {
    return when (reason) {
        NotEnoughBalanceToPayFees -> resourceManager.notSufficientBalanceToPayFeeErrorMessage()
    }
}
