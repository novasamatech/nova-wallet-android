package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.removeVotes.validations

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias RemoteVotesValidationSystem = ValidationSystem<RemoveVotesValidationPayload, RemoveVotesValidationFailure>

fun ValidationSystem.Companion.removeVotesValidationSystem(): RemoteVotesValidationSystem = ValidationSystem {
    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            RemoveVotesValidationFailure.NotEnoughToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.availableToPayFees,
                fee = context.fee
            )
        }
    )
}
