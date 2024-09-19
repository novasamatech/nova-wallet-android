package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.MaximumTrackVotesNotReachedValidation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.NotDelegatingInTrackValidation
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.ReferendumIsOngoingValidation
import io.novafoundation.nova.feature_wallet_api.domain.validation.hasEnoughFreeBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias VoteTinderGovValidationSystem = ValidationSystem<VoteTinderGovValidationPayload, VoteTinderGovValidationFailure>
typealias VoteTinderGovValidationSystemBuilder = ValidationSystemBuilder<VoteTinderGovValidationPayload, VoteTinderGovValidationFailure>

fun ValidationSystem.Companion.voteTinderGovValidationSystem(
    governanceSourceRegistry: GovernanceSourceRegistry,
    governanceSharedState: GovernanceSharedState,
): VoteTinderGovValidationSystem = ValidationSystem {
    hasEnoughFreeBalance(
        asset = { it.asset },
        fee = { it.fee },
        requestedAmount = { it.maxAmount },
        error = VoteTinderGovValidationFailure::AmountIsTooBig
    )

    sufficientBalance(
        fee = { it.fee },
        available = { it.asset.transferable },
        error = { context ->
            VoteTinderGovValidationFailure.NotEnoughToPayFees(
                chainAsset = context.payload.asset.token.configuration,
                maxUsable = context.maxUsable,
                fee = context.fee
            )
        }
    )

    referendumIsOngoing()

    notDelegatingInTrack()

    maximumTrackVotesNotReached(governanceSourceRegistry, governanceSharedState)
}

fun VoteTinderGovValidationSystemBuilder.maximumTrackVotesNotReached(
    governanceSourceRegistry: GovernanceSourceRegistry,
    governanceSharedState: GovernanceSharedState,
) {
    validate(
        MaximumTrackVotesNotReachedValidation(
            governanceSourceRegistry,
            governanceSharedState,
            VoteTinderGovValidationFailure::MaxTrackVotesReached
        )
    )
}

fun VoteTinderGovValidationSystemBuilder.notDelegatingInTrack() {
    validate(
        NotDelegatingInTrackValidation { VoteTinderGovValidationFailure.AlreadyDelegatingVotes }
    )
}

fun VoteTinderGovValidationSystemBuilder.referendumIsOngoing() {
    validate(ReferendumIsOngoingValidation(VoteTinderGovValidationFailure::ReferendumCompleted))
}
