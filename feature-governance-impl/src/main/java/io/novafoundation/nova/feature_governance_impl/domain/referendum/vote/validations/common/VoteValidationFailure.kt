package io.novafoundation.nova.feature_governance_impl.domain.referendum.vote.validations.common

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughFreeBalanceError
import io.novafoundation.nova.feature_wallet_api.domain.validation.NotEnoughToPayFeesError
import java.math.BigInteger

interface VoteValidationFailure {

    interface NotEnoughToPayFees : VoteValidationFailure, NotEnoughToPayFeesError

    interface AmountIsTooBig : VoteValidationFailure, NotEnoughFreeBalanceError

    interface ReferendumCompleted : VoteValidationFailure {
        val referendumId: ReferendumId
    }

    interface AlreadyDelegatingVotes : VoteValidationFailure

    interface MaxTrackVotesReached : VoteValidationFailure {
        val max: BigInteger
    }
}
