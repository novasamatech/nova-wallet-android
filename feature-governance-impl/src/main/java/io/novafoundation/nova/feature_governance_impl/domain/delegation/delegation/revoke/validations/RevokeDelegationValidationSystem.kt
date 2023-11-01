package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias RevokeDelegationValidationSystem = ValidationSystem<RevokeDelegationValidationPayload, RevokeDelegationValidationFailure>

fun ValidationSystem.Companion.revokeDelegationValidationSystem(): RevokeDelegationValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            RevokeDelegationValidationFailure.NotEnoughToPayFees(
                chainAsset = payload.asset.token.configuration,
                maxUsable = leftForFees,
                fee = payload.fee
            )
        }
    )
}
