package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response

import java.math.BigInteger

class StandardVoteRemote(val aye: Boolean, val vote: VoteRemote)

class VoteRemote(val amount: BigInteger, val conviction: String)
