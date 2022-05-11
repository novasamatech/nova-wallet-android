package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.start.validations

import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigDecimal

sealed class StartParachainStakingValidationFailure {

    object NotEnoughBalanceToPayFees : StartParachainStakingValidationFailure()

    sealed class TooLowStake(val minimumStake: BigDecimal, val asset: Asset): StartParachainStakingValidationFailure() {

        class TooLowDelegation(minimumStake: BigDecimal, asset: Asset): TooLowStake(minimumStake, asset)

        class TooLowTotalStake(minimumStake: BigDecimal, asset: Asset): TooLowStake(minimumStake, asset)

        class WontReceiveRewards(minimumStake: BigDecimal, asset: Asset): TooLowStake(minimumStake, asset)
    }
}
