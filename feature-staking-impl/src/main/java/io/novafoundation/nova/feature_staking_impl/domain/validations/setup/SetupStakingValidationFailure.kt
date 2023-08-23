package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

sealed class SetupStakingValidationFailure {

    object CannotPayFee : SetupStakingValidationFailure()

    object NotEnoughStakeable : SetupStakingValidationFailure()

    class AmountLessThanMinimum(override val context: StakingMinimumBondError.Context): SetupStakingValidationFailure(), StakingMinimumBondError

    object MaxNominatorsReached : SetupStakingValidationFailure()
}
