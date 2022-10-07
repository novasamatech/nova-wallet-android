package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

interface GovernanceSourceRegistry {

    suspend fun sourceFor(chainId: ChainId): GovernanceSource
}
