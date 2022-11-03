package io.novafoundation.nova.feature_governance_impl.data

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SingleAssetSharedState

private const val GOVERNANCE_SHARED_STATE = "GOVERNANCE_SHARED_STATE"

class GovernanceSharedState(
    chainRegistry: ChainRegistry,
    preferences: Preferences,
) : SingleAssetSharedState(
    preferences = preferences,
    chainRegistry = chainRegistry,
    filter = { chain, asset -> chain.governance != Chain.Governance.NONE && asset.isUtilityAsset },
    preferencesKey = GOVERNANCE_SHARED_STATE
)
