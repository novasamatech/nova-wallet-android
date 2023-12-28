package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.stakeableAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.positiveAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance
import java.math.BigDecimal

typealias StartParachainStakingValidationSystem = ValidationSystem<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>
typealias StartParachainStakingValidationSystemBuilder = ValidationSystemBuilder<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>
typealias StartParachainStakingValidation = Validation<StartParachainStakingValidationPayload, StartParachainStakingValidationFailure>

fun ValidationSystem.Companion.parachainStakingStart(
    minimumDelegationValidationFactory: MinimumDelegationValidationFactory,
    noPendingRevokeValidationFactory: NoPendingRevokeValidationFactory,
): StartParachainStakingValidationSystem = ValidationSystem {
    with(minimumDelegationValidationFactory) {
        minimumDelegation()
    }

    with(noPendingRevokeValidationFactory) {
        noPendingRevoke()
    }

    activeCollator()

    positiveAmount(
        amount = { it.amount },
        error = { StartParachainStakingValidationFailure.NotPositiveAmount }
    )

    enoughToPayFees()

    enoughStakeable()
}

private fun StartParachainStakingValidationSystemBuilder.enoughToPayFees() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees }
    )
}

private fun StartParachainStakingValidationSystemBuilder.enoughStakeable() {
    sufficientBalance(
        fee = { it.fee },
        available = { it.stakeableAmount() },
        amount = { it.amount },
        error = { StartParachainStakingValidationFailure.NotEnoughBalanceToPayFees }
    )
}

private fun StartParachainStakingValidationPayload.stakeableAmount(): BigDecimal {
    return delegatorState.stakeableAmount(asset.freeInPlanks)
}
