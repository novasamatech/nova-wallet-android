package io.novafoundation.nova.feature_governance_impl.data.source

import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class RealGovernanceSourceRegistry(
    private val governanceV2Source: GovernanceSource,
    private val governanceV1Source: GovernanceSource,
) : GovernanceSourceRegistry {

    override suspend fun sourceFor(option: SupportedGovernanceOption): GovernanceSource {
        return when (option.additional.governanceType) {
            Chain.Governance.V1 -> governanceV1Source
            Chain.Governance.V2 -> governanceV2Source
        }
    }
}
