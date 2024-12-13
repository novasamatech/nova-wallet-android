package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.tindergov

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common.VoteValidationFailure
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger

sealed class VoteTinderGovValidationFailure : VoteValidationFailure {

    class NotEnoughToPayFees(
        override val chainAsset: Chain.Asset,
        override val maxUsable: BigDecimal,
        override val fee: BigDecimal
    ) : VoteTinderGovValidationFailure(), VoteValidationFailure.NotEnoughToPayFees

    class AmountIsTooBig(
        override val chainAsset: Chain.Asset,
        override val freeAfterFees: BigDecimal,
    ) : VoteTinderGovValidationFailure(), VoteValidationFailure.AmountIsTooBig

    class ReferendumCompleted(override val referendumId: ReferendumId) : VoteTinderGovValidationFailure(), VoteValidationFailure.ReferendumCompleted

    object AlreadyDelegatingVotes : VoteTinderGovValidationFailure(), VoteValidationFailure.AlreadyDelegatingVotes

    class MaxTrackVotesReached(override val max: BigInteger) : VoteTinderGovValidationFailure(), VoteValidationFailure.MaxTrackVotesReached
}
