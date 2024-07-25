package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.common.validation.Validation
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.common.validation.ValidationSystemBuilder
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.domain.validation.hasEnoughFreeBalance
import io.novafoundation.nova.feature_wallet_api.domain.validation.sufficientBalance

typealias VoteReferendumValidationSystem = ValidationSystem<VoteReferendumValidationPayload, VoteReferendumValidationFailure>
typealias VoteReferendumValidation = Validation<VoteReferendumValidationPayload, VoteReferendumValidationFailure>
typealias VoteReferendumValidationSystemBuilder = ValidationSystemBuilder<VoteReferendumValidationPayload, VoteReferendumValidationFailure>

fun ValidationSystem.Companion.voteReferendumValidationSystem(
    governanceSourceRegistry: GovernanceSourceRegistry,
    governanceSharedState: GovernanceSharedState,
): VoteReferendumValidationSystem = ValidationSystem {
    hasEnoughFreeBalance(
        asset = { it.asset },
        fee = { it.fee },
        requestedAmount = { it.voteAmount },
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
