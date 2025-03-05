package io.novafoundation.nova.feature_deep_link_building.presentation

import android.net.Uri
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.appendNullableQueryParameter
import io.novafoundation.nova.common.utils.doIf
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class ReferendumDeepLinkData(
    val chainId: String,
    val referendumId: BigInteger,
    val governanceType: Chain.Governance
)

class ReferendumDetailsDeepLinkConfigurator(
    private val resourceManager: ResourceManager
) : DeepLinkConfigurator<ReferendumDeepLinkData> {

    val deepLinkPrefix = "/open/gov"
    val chainIdParam = "chainId"
    val referendumIdParam = "id"
    val governanceTypeParam = "type"

    override fun configure(payload: ReferendumDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        // We not add Polkadot chain id to simplify deep link
        val appendChainIdParam = payload.chainId != ChainGeneses.POLKADOT

        return buildLink(resourceManager, deepLinkPrefix, type)
            .doIf(appendChainIdParam) { appendQueryParameter(chainIdParam, payload.chainId) }
            .appendQueryParameter(referendumIdParam, payload.referendumId.toString())
            .appendNullableQueryParameter(governanceTypeParam, payload.governanceType.let(::mapGovTypeToParams))
            .build()
    }

    private fun mapGovTypeToParams(govType: Chain.Governance): String? {
        return when (govType) {
            Chain.Governance.V1 -> 1.toString()
            Chain.Governance.V2 -> null // We handle null case as Gov2 by default and it's not necessary to put it in link
        }
    }
}
