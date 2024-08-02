package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations

import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughFreeBalanceError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

sealed class VoteReferendumValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : VoteReferendumValidationFailure(), NotEnoughToPayFeesError

    class AmountIsTooBig(
        override val chainAsset: Chain.Asset,
        override val freeAfterFees: BigDecimal,
    ) : VoteReferendumValidationFailure(), NotEnoughFreeBalanceError

    object ReferendumCompleted : VoteReferendumValidationFailure()

    object AlreadyDelegatingVotes : VoteReferendumValidationFailure()

    class MaxTrackVotesReached(val max: BigInteger) : VoteReferendumValidationFailure()

    object AbstainInvalidConviction : VoteReferendumValidationFailure()
}
