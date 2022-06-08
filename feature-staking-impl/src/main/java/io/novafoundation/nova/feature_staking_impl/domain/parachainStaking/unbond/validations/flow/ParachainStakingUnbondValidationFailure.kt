package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.flow

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

sealed class ParachainStakingUnbondValidationFailure {

    object NotPositiveAmount : ParachainStakingUnbondValidationFailure()

    object NotEnoughBalanceToPayFees : ParachainStakingUnbondValidationFailure()
    object NotEnoughBondedToUnbond : ParachainStakingUnbondValidationFailure()

    object AlreadyHasDelegationRequestToCollator : ParachainStakingUnbondValidationFailure()

    sealed class TooLowRemainingBond(val minimumRequired: BigDecimal, val asset: Asset) : ParachainStakingUnbondValidationFailure() {

        class WillBeAddedToUnbondings(val newAmount: BigDecimal, minimumStake: BigDecimal, asset: Asset) : TooLowRemainingBond(minimumStake, asset)

        class WontReceiveRewards(minimumStake: BigDecimal, asset: Asset) : TooLowRemainingBond(minimumStake, asset)
    }
}
