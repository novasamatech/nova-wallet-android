package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.data.repository.ReferendumUrlConstructor
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class GovV2DAppsRepository : GovernanceDAppsRepository {

    override suspend fun getDAppUrlConstructorsFor(chain: Chain): List<ReferendumUrlConstructor> {
        return listOfNotNull(
            KnownChainDAppConstructor(chain, ::PolkassemblyUrlConstructor),
            KnownChainDAppConstructor(chain, ::SubsquareUrlConstructor)
        )
    }
}

private fun KnownChainDAppConstructor(chain: Chain, builder: (chainPrefix: String) -> ReferendumUrlConstructor): ReferendumUrlConstructor? {
    val chainPrefix = when (chain.id) {
        Chain.Geneses.POLKADOT -> "polkadot"
        Chain.Geneses.KUSAMA -> "kusama"
        Chain.Geneses.GOV2_TESTNET -> "governance2-testnet"
        else -> null
    }

    return chainPrefix?.let(builder)
}

private class PolkassemblyUrlConstructor(chainPrefix: String) : ReferendumUrlConstructor {

    override val baseUrl: String = "https://${chainPrefix}.polkassembly.io"

    override val metadataSearchUrl: String = "https://polkadot.polkassembly.io/"

    override fun urlFor(referendumId: ReferendumId): String {
        return "$baseUrl/referendum/${referendumId.value}"
    }
}

private class SubsquareUrlConstructor(chainPrefix: String) : ReferendumUrlConstructor {

    override val baseUrl: String = "https://${chainPrefix}.subsquare.io"

    override val metadataSearchUrl: String = "https://polkadot.subsquare.io/"

    override fun urlFor(referendumId: ReferendumId): String {
        return "$baseUrl/democracy/referendum/${referendumId.value}"
    }
}
