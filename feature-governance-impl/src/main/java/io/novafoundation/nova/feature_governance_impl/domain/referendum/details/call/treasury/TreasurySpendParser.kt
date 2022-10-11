package io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.treasury

import io.novafoundation.nova.common.data.network.runtime.binding.bindNonce
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.instanceOf
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.PreImage
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_impl.domain.referendum.details.call.ReferendumCallParser
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class TreasurySpendParser : ReferendumCallParser {

    override suspend fun parse(preImage: PreImage, chainId: ChainId): ReferendumCall? = runCatching {
        val call = preImage.call

        if (!call.instanceOf(Modules.TREASURY, "spend")) return null

        val amount = bindNonce(call.arguments["amount"])

        ReferendumCall.TreasuryRequest(amount)
    }.getOrNull()
}
