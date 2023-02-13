package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.delegation.stats.response

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import java.math.BigInteger

class StandardVoteRemote(val aye: Boolean, val vote: VoteRemote)

class SplitVoteRemote(val ayeAmount: Balance, val nayAmount: Balance)

class SplitAbstainVoteRemote(val ayeAmount: Balance, val nayAmount: Balance, val abstainAmount: Balance)

class VoteRemote(val amount: BigInteger, val conviction: String)
