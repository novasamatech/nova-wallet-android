package io.novafoundation.nova.feature_governance_api.data

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MutableGovernanceState {
    fun update(chainId: String, assetId: Int, governanceType: Chain.Governance)
}
