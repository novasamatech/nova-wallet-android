package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.SelectableAssetAdditionalData
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState

interface GovernanceSourceRegistry {

    suspend fun sourceFor(option: SupportedGovernanceOption): GovernanceSource

    suspend fun sourceFor(option: Chain.Governance): GovernanceSource
}

typealias SupportedGovernanceOption = SelectedAssetOptionSharedState.SupportedAssetOption<GovernanceAdditionalState>

interface GovernanceAdditionalState : SelectableAssetAdditionalData {

    val governanceType: Chain.Governance
}
