package io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators

import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class ReferendumDeepLinkData(
    val chainId: String,
    val referendumId: BigInteger,
    val governanceType: Chain.Governance
)

interface ReferendumDetailsDeepLinkConfigurator : DeepLinkConfigurator<ReferendumDeepLinkData> {

    companion object {
        const val ACTION = "open"
        const val SCREEN = "gov"
        const val PREFIX = "/$ACTION/$SCREEN"
        const val CHAIN_ID_PARAM = "chainId"
        const val REFERENDUM_ID_PARAM = "id"
        const val GOVERNANCE_TYPE_PARAM = "type"
    }
}
