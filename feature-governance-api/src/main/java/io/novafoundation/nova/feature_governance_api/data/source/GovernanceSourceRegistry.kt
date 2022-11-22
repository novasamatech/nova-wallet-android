package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.AssetSharedStateAdditionalData
import io.novafoundation.nova.runtime.state.GenericSingleAssetSharedState

interface GovernanceSourceRegistry {

    suspend fun sourceFor(option: SupportedGovernanceOption): GovernanceSource
}


typealias SupportedGovernanceOption = GenericSingleAssetSharedState.SupportedAssetOption<GovernanceAdditionalState>

class GovernanceAdditionalState(
    val governanceType: Chain.Governance,
    private val shouldIncludeSuffix: Boolean
) : AssetSharedStateAdditionalData {

    override val identifier: String = governanceType.name

    override fun format(resourceManager: ResourceManager): String? {
        if (!shouldIncludeSuffix) return null

        return when (governanceType) {
            Chain.Governance.V1 -> "Governance V1"
            Chain.Governance.V2 -> "OpenGov"
        }
    }
}
