package io.novafoundation.nova.feature_governance_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceAdditionalState
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.GenericSingleAssetSharedState

private const val GOVERNANCE_SHARED_STATE = "GOVERNANCE_SHARED_STATE"

class GovernanceSharedState(
    chainRegistry: ChainRegistry,
    preferences: Preferences,
) : GenericSingleAssetSharedState<GovernanceAdditionalState>(
    preferences = preferences,
    chainRegistry = chainRegistry,
    supportedOptions = { chain, asset ->
        if (asset.isUtilityAsset) {
            val shouldIncludeSuffix = chain.governance.size > 1
            chain.governance.map { GovernanceAdditionalState(it, shouldIncludeSuffix) }
        } else {
            emptyList()
        }
    },
    preferencesKey = GOVERNANCE_SHARED_STATE
)
