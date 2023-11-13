package io.novafoundation.nova.feature_governance_impl.domain.referendum.unlock.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias UnlockReferendumValidationSystem = ValidationSystem<UnlockReferendumValidationPayload, UnlockGovernanceValidationFailure>

fun ValidationSystem.Companion.unlockReferendumValidationSystem(): UnlockReferendumValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            UnlockGovernanceValidationFailure.NotEnoughToPayFees(
                chainAsset = payload.asset.token.configuration,
                maxUsable = leftForFees,
                fee = payload.fee
            )
        }
    )
}
