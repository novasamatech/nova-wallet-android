package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias ParachainStakingRedeemValidationSystem = ValidationSystem<ParachainStakingRedeemValidationPayload, ParachainStakingRedeemValidationFailure>

fun ValidationSystem.Companion.parachainStakingRedeem(): ParachainStakingRedeemValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { ParachainStakingRedeemValidationFailure.NotEnoughBalanceToPayFees }
    )
}
