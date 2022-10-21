package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias VoteReferendumValidationSystem = ValidationSystem<VoteReferendumValidationPayload, VoteReferendumValidationFailure>
typealias VoteReferendumValidation = Validation<VoteReferendumValidationPayload, VoteReferendumValidationFailure>
typealias VoteReferendumValidationSystemBuilder = ValidationSystemBuilder<VoteReferendumValidationPayload, VoteReferendumValidationFailure>

fun ValidationSystem.Companion.voteReferendumValidationSystem(
    governanceSourceRegistry: GovernanceSourceRegistry
) : VoteReferendumValidationSystem = ValidationSystem {
    hasEnoughFreeBalance()

    sufficientBalance(
        fee = { it.fee },
        amount = { it.voteAmount },
        available = { it.asset.transferable },
        error = { payload, leftForFees ->
            VoteReferendumValidationFailure.NotEnoughToPayFees(
                chainAsset = payload.asset.token.configuration,
                availableToPayFees = leftForFees,
                fee = payload.fee
            )
        }
    )

    referendumIsOngoing()

    notDelegatingInTrack()

    maximumTrackVotesNotReached(governanceSourceRegistry)
}

