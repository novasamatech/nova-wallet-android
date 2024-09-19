package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.TransformedFailure.Default
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleAlreadyDelegatingVotes
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleAmountIsTooBig
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleMaxTrackVotesReached
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.handleReferendumCompleted
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import java.math.BigDecimal

fun handleVoteTinderGovValidationFailure(
    failure: VoteTinderGovValidationFailure,
    actions: ValidationFlowActions<VoteTinderGovValidationPayload>,
    resourceManager: ResourceManager
): TransformedFailure {
    return when (failure) {
        is VoteTinderGovValidationFailure.NotEnoughToPayFees -> Default(handleNotEnoughFeeError(failure, resourceManager))

        VoteTinderGovValidationFailure.AlreadyDelegatingVotes -> handleAlreadyDelegatingVotes(resourceManager)

        is VoteTinderGovValidationFailure.AmountIsTooBig -> {
            val titleAndMessage = handleAmountIsTooBig(resourceManager, failure).titleAndMessage
            TransformedFailure.Custom(
                CustomDialogDisplayer.Payload(
                    title = titleAndMessage.first,
                    message = titleAndMessage.second,
                    cancelAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_cancel),
                        action = { }
                    ),
                    okAction = CustomDialogDisplayer.Payload.DialogAction(
                        title = resourceManager.getString(R.string.common_use_max),
                        action = {
                            actions.revalidate { payload ->
                                cutBasketAmount(payload, failure.freeAfterFees)
                            }
                        }
                    ),
                    customStyle = R.style.AccentAlertDialogTheme
                )
            )
        }

        is VoteTinderGovValidationFailure.MaxTrackVotesReached -> handleMaxTrackVotesReached(resourceManager, failure)

        is VoteTinderGovValidationFailure.ReferendumCompleted -> handleReferendumCompleted(resourceManager, failure)
    }
}

private fun cutBasketAmount(payload: VoteTinderGovValidationPayload, maxAvailable: BigDecimal): VoteTinderGovValidationPayload {
    val maxAvailablePlanks = payload.asset.token.planksFromAmount(maxAvailable)
    val newBasket = payload.basket.map {
        if (it.amount > maxAvailablePlanks) {
            it.copy(amount = maxAvailablePlanks)
        } else {
            it
        }
    }

    return payload.copy(basket = newBasket)
}
