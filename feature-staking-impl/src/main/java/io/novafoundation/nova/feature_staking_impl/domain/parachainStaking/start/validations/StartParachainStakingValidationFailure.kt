package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

sealed class StartParachainStakingValidationFailure {

    object NotPositiveAmount : StartParachainStakingValidationFailure()

    object NotEnoughBalanceToPayFees : StartParachainStakingValidationFailure()

    object NotEnoughStakeableBalance : StartParachainStakingValidationFailure()

    object PendingRevoke : StartParachainStakingValidationFailure()

    object CollatorIsNotActive : StartParachainStakingValidationFailure()

    sealed class TooLowStake(val minimumStake: BigDecimal, val asset: Asset) : StartParachainStakingValidationFailure() {

        class TooLowDelegation(minimumStake: BigDecimal, asset: Asset, val strictGreaterThan: Boolean) : TooLowStake(minimumStake, asset)

        class TooLowTotalStake(minimumStake: BigDecimal, asset: Asset) : TooLowStake(minimumStake, asset)

        class WontReceiveRewards(minimumStake: BigDecimal, asset: Asset) : TooLowStake(minimumStake, asset)
    }
}
