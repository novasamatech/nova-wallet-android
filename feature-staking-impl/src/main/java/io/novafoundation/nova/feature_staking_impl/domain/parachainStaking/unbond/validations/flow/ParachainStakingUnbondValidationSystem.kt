package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias ParachainStakingUnbondValidationSystem = ValidationSystem<ParachainStakingUnbondValidationPayload, ParachainStakingUnbondValidationFailure>
typealias ParachainStakingUnbondValidation = Validation<ParachainStakingUnbondValidationPayload, ParachainStakingUnbondValidationFailure>

fun ValidationSystem.Companion.parachainStakingUnbond(
    remainingUnbondValidationFactory: RemainingUnbondValidationFactory,
    noExistingDelegationRequestsToCollatorValidationFactory: NoExistingDelegationRequestsToCollatorValidationFactory,
): ParachainStakingUnbondValidationSystem = ValidationSystem {
    with(remainingUnbondValidationFactory) {
        validRemainingUnbond()
    }

    with(noExistingDelegationRequestsToCollatorValidationFactory) {
        noExistingDelegationRequestsToCollator()
    }

    positiveAmount(
        amount = { it.amount },
        error = { ParachainStakingUnbondValidationFailure.NotPositiveAmount }
    )

    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { ParachainStakingUnbondValidationFailure.NotEnoughBalanceToPayFees }
    )
}
