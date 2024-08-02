package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.subsquare.v2.response

import java.math.BigInteger

class ReferendumVoteV2Response(
    val isSplitAbstain: Boolean,
    val abstainVotes: BigInteger?
)
