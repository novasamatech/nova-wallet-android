package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface ReferendumCallParser {

    suspend fun parse(preImage: PreImage, chainId: ChainId): ReferendumCall?
}
