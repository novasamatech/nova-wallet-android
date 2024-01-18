package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rebond.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias ParachainStakingRebondValidationSystem = ValidationSystem<ParachainStakingRebondValidationPayload, ParachainStakingRebondValidationFailure>

fun ValidationSystem.Companion.parachainStakingRebond(): ParachainStakingRebondValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { ParachainStakingRebondValidationFailure.NotEnoughBalanceToPayFees }
    )
}
