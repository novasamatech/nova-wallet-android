package io.novafoundation.nova.feature_governance_api.data.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigInteger

class VotingPower(
    val chainId: ChainId,
    val amount: BigInteger,
    val conviction: Conviction
)
