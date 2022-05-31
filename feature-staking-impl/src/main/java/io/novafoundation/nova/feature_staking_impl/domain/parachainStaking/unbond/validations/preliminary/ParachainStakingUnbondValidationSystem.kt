package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder

typealias ParachainStakingUnbondPreliminaryValidationSystem =
    ValidationSystem<ParachainStakingUnbondPreliminaryValidationPayload, ParachainStakingUnbondPreliminaryValidationFailure>
typealias ParachainStakingUnbondPreliminaryValidation =
    Validation<ParachainStakingUnbondPreliminaryValidationPayload, ParachainStakingUnbondPreliminaryValidationFailure>

typealias ParachainStakingUnbondPreliminaryValidationSystemBuilder =
    ValidationSystemBuilder<ParachainStakingUnbondPreliminaryValidationPayload, ParachainStakingUnbondPreliminaryValidationFailure>

fun ValidationSystem.Companion.parachainStakingPreliminaryUnbond(
    anyAvailableCollatorForUnbondValidationFactory: AnyAvailableCollatorForUnbondValidationFactory
): ParachainStakingUnbondPreliminaryValidationSystem = ValidationSystem {
    with(anyAvailableCollatorForUnbondValidationFactory) {
        anyAvailableCollatorForUnbond()
    }
}
