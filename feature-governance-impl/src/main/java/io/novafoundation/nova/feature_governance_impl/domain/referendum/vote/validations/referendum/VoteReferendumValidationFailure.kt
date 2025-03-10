package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.referendum

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.VoteValidationFailure
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

sealed class VoteReferendumValidationFailure : VoteValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : VoteReferendumValidationFailure(), VoteValidationFailure.NotEnoughToPayFees

    class AmountIsTooBig(
        override val chainAsset: Chain.Asset,
        override val freeAfterFees: BigDecimal,
    ) : VoteReferendumValidationFailure(), VoteValidationFailure.AmountIsTooBig

    class ReferendumCompleted(override val referendumId: ReferendumId) : VoteReferendumValidationFailure(), VoteValidationFailure.ReferendumCompleted

    object AlreadyDelegatingVotes : VoteReferendumValidationFailure(), VoteValidationFailure.AlreadyDelegatingVotes

    class MaxTrackVotesReached(override val max: BigInteger) : VoteReferendumValidationFailure(), VoteValidationFailure.MaxTrackVotesReached

    object AbstainInvalidConviction : VoteReferendumValidationFailure()
}
