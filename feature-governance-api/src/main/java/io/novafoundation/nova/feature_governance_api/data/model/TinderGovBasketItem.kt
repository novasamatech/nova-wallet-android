package io.novafoundation.nova.feature_governance_api.data.model

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigInteger

class TinderGovBasketItem(
    val metaId: Long,
    val chainId: String,
    val referendumId: ReferendumId,
    val voteType: VoteType,
    val conviction: Conviction,
    val amount: BigInteger
)
