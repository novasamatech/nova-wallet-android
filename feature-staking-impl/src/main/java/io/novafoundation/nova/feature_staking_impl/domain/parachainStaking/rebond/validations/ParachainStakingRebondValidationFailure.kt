package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations

sealed class ParachainStakingRebondValidationFailure {

    object NotEnoughBalanceToPayFees : ParachainStakingRebondValidationFailure()
}
