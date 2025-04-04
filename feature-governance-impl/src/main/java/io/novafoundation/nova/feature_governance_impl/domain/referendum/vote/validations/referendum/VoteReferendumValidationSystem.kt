package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.MaximumTrackVotesNotReachedValidation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.NotDelegatingInTrackValidation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.ReferendumIsOngoingValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.hasEnoughBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias VoteReferendumValidationSystem = ValidationSystem<VoteReferendaValidationPayload, VoteReferendumValidationFailure>
typealias VoteReferendumValidation = Validation<VoteReferendaValidationPayload, VoteReferendumValidationFailure>
typealias VoteReferendumValidationSystemBuilder = ValidationSystemBuilder<VoteReferendaValidationPayload, VoteReferendumValidationFailure>

fun ValidationSystem.Companion.voteReferendumValidationSystem(
    governanceSourceRegistry: GovernanceSourceRegistry,
    governanceSharedState: GovernanceSharedState,
): VoteReferendumValidationSystem = ValidationSystem {
    hasEnoughBalance(
        availableBalance = { it.maxAvailableAmount },
        requestedAmount = { it.amount },
        chainAsset = { it.asset.token.configuration },
        error = VoteReferendumValidationFailure::AmountIsTooBig
    )

    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            VoteReferendumValidationFailure.NotEnoughToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )

    referendumIsOngoing()

    notDelegatingInTrack()

    maximumTrackVotesNotReached(governanceSourceRegistry, governanceSharedState)

    abstainConvictionValid()
}

fun VoteReferendumValidationSystemBuilder.maximumTrackVotesNotReached(
    governanceSourceRegistry: GovernanceSourceRegistry,
    governanceSharedState: GovernanceSharedState,
) {
    validate(
        MaximumTrackVotesNotReachedValidation(
            governanceSourceRegistry,
            governanceSharedState,
            VoteReferendumValidationFailure::MaxTrackVotesReached
        )
    )
}

fun VoteReferendumValidationSystemBuilder.notDelegatingInTrack() {
    validate(
        NotDelegatingInTrackValidation { VoteReferendumValidationFailure.AlreadyDelegatingVotes }
    )
}

fun VoteReferendumValidationSystemBuilder.referendumIsOngoing() {
    validate(ReferendumIsOngoingValidation(VoteReferendumValidationFailure::ReferendumCompleted))
}
