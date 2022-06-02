package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations

sealed class ParachainStakingRedeemValidationFailure {

    object NotEnoughBalanceToPayFees : ParachainStakingRedeemValidationFailure()
}
