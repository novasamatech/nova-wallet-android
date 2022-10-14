package io.novafoundation.nova.feature_governance_impl.data.repository.v2

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.repository.GovernanceDAppsRepository
import io.novafoundation.nova.feature_governance_api.data.repository.ReferendumUrlConstructor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class GovV2DAppsRepository : GovernanceDAppsRepository {

    override suspend fun getDAppUrlConstructorsFor(chain: Chain): List<ReferendumUrlConstructor> {
        return listOf(
            PolkassemblyUrlConstructor(chain),
            SubsquareUrlConstructor(chain)
        )
    }
}

private class PolkassemblyUrlConstructor(chain: Chain): ReferendumUrlConstructor {

    override val baseUrl: String = "https://${chain.name}.polkassembly.io"

    override fun urlFor(referendumId: ReferendumId): String {
        return "$baseUrl/referendum/${referendumId.value}"
    }
}

private class SubsquareUrlConstructor(chain: Chain): ReferendumUrlConstructor {

    override val baseUrl: String = "https://${chain.name}.subsquare.io"

    override fun urlFor(referendumId: ReferendumId): String {
        return "$baseUrl/democracy/referendum/${referendumId.value}"
    }
}
