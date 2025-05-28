package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details.deeplink

import android.net.Uri
import io.novafoundation.nova.common.utils.doIf
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.DeepLinkConfigurator
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.LinkBuilderFactory
import io.novafoundation.nova.feature_deep_linking.presentation.configuring.addParamIfNotNull
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDeepLinkData
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator.Companion.ACTION
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator.Companion.CHAIN_ID_PARAM
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator.Companion.GOVERNANCE_TYPE_PARAM
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator.Companion.REFERENDUM_ID_PARAM
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.deeplink.configurators.ReferendumDetailsDeepLinkConfigurator.Companion.SCREEN
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class RealReferendumDetailsDeepLinkConfigurator(
    private val linkBuilderFactory: LinkBuilderFactory
) : ReferendumDetailsDeepLinkConfigurator {

    override fun configure(payload: ReferendumDeepLinkData, type: DeepLinkConfigurator.Type): Uri {
        // We not add Polkadot chain id to simplify deep link
        val appendChainIdParam = payload.chainId != ChainGeneses.POLKADOT

        return linkBuilderFactory.newLink(type)
            .setAction(ACTION)
            .setScreen(SCREEN)
            .doIf(appendChainIdParam) { addParam(CHAIN_ID_PARAM, payload.chainId) }
            .addParam(REFERENDUM_ID_PARAM, payload.referendumId.toString())
            .addParamIfNotNull(GOVERNANCE_TYPE_PARAM, payload.governanceType.let(::mapGovTypeToParams))
            .build()
    }

    private fun mapGovTypeToParams(govType: Chain.Governance): String? {
        return when (govType) {
            Chain.Governance.V1 -> 1.toString()
            Chain.Governance.V2 -> null // We handle null case as Gov2 by default and it's not necessary to put it in link
        }
    }
}
