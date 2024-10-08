package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleAmountIsTooBig
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleMaxTrackVotesReached
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleReferendumCompleted
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction

fun handleVoteReferendumValidationFailure(
    failure: VoteReferendumValidationFailure,
    actions: ValidationFlowActions<VoteReferendaValidationPayload>,
    resourceManager: ResourceManager
): TransformedFailure {
    return when (failure) {
        is VoteReferendumValidationFailure.NotEnoughToPayFees -> Default(handleNotEnoughFeeError(failure, resourceManager))

        VoteReferendumValidationFailure.AlreadyDelegatingVotes -> Default(
            resourceManager.getString(R.string.refrendum_vote_already_delegating_title) to
                resourceManager.getString(R.string.refrendum_vote_already_delegating_message)
        )

        is VoteReferendumValidationFailure.AmountIsTooBig -> handleAmountIsTooBig(resourceManager, failure)

        is VoteReferendumValidationFailure.MaxTrackVotesReached -> handleMaxTrackVotesReached(resourceManager, failure)

        is VoteReferendumValidationFailure.ReferendumCompleted -> handleReferendumCompleted(resourceManager, failure)

        VoteReferendumValidationFailure.AbstainInvalidConviction -> {
            TransformedFailure.Custom(
                CustomDialogDisplayer.Payload(
                    title = resourceManager.getString(R.string.referendum_abstain_vote_invalid_conviction_title),
                    message = resourceManager.getString(R.string.referendum_abstain_vote_invalid_conviction_subtitle),
                    cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_cancel),
                        action = { }
                    ),
                    okAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_continue),
                        action = {
                            actions.resumeFlow { payload ->
                                payload.copy(conviction = Conviction.None)
                            }
                        }
                    ),
                    customStyle = R.style.AccentAlertDialogTheme
                )
            )
        }
    }
}
