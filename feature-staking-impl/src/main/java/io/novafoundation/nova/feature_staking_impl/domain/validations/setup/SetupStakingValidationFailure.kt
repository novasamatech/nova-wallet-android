package io.novafoundation.nova.feature_staking_impl.domain.validations.setup

import java.math.BigDecimal

sealed class SetupStakingValidationFailure {

    object CannotPayFee : SetupStakingValidationFailure()

    object NotEnoughStakeable : SetupStakingValidationFailure()

    class TooSmallAmount(val threshold: BigDecimal) : SetupStakingValidationFailure()

    object MaxNominatorsReached : SetupStakingValidationFailure()
}
