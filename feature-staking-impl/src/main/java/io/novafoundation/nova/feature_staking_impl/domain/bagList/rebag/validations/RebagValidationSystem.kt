package io.novafoundation.nova.feature_staking_impl.domain.bagList.rebag.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias RebagValidationSystem = ValidationSystem<RebagValidationPayload, RebagValidationFailure>

fun ValidationSystem.Companion.rebagValidationSystem(): RebagValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            RebagValidationFailure.NotEnoughToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.availableToPayFees,
                fee = context.fee
            )
        }
    )
}
