package io.novafoundation.nova.feature_governance_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceAdditionalState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectableSingleAssetSharedState

private const val GOVERNANCE_SHARED_STATE = "GOVERNANCE_SHARED_STATE"

class GovernanceSharedState(
    chainRegistry: ChainRegistry,
    preferences: Preferences,
) : SelectableSingleAssetSharedState<GovernanceAdditionalState>(
    preferences = preferences,
    chainRegistry = chainRegistry,
    supportedOptions = { chain, asset ->
        if (asset.isUtilityAsset) {
            val shouldIncludeSuffix = chain.governance.size > 1
            chain.governance.map { RealGovernanceAdditionalState(it, shouldIncludeSuffix) }
        } else {
            emptyList()
        }
    },
    preferencesKey = GOVERNANCE_SHARED_STATE
)

class RealGovernanceAdditionalState(
    override val governanceType: Chain.Governance,
    private val shouldIncludeSuffix: Boolean
) : GovernanceAdditionalState {

    override val identifier: String = governanceType.name

    override fun format(resourceManager: ResourceManager): String? {
        if (!shouldIncludeSuffix) return null

        return when (governanceType) {
            Chain.Governance.V1 -> resourceManager.getString(R.string.assets_balance_details_locks_democrac_v1)
            Chain.Governance.V2 -> resourceManager.getString(R.string.assets_balance_details_locks_democrac_v2)
        }
    }
}
