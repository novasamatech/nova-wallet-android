package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias StartParachainStakingValidationSystem = ValidationSystem<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>
typealias StartParachainStakingValidation = Validation<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>

fun ValidationSystem.Companion.parachainStakingStart(
    minimumDelegationValidationFactory: MinimumDelegationValidationFactory,
): StartParachainStakingValidationSystem = ValidationSystem {
    with(minimumDelegationValidationFactory) {
        minimumDelegation()
    }

    activeCollatorValidation()

    positiveAmount(
        amount = { it.amount },
        error = { StartParachainStakingValidationFailure.NotPositiveAmount }
    )

    sufficientBalance(
        fee = { it.fee },
        amount = { it.amount },
        available = { it.asset.transferable },
        error = { StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees }
    )
}
