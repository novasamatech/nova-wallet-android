package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary

sealed class ParachainStakingUnbondPreliminaryValidationFailure {

    object NoAvailableCollators : ParachainStakingUnbondPreliminaryValidationFailure()
}
