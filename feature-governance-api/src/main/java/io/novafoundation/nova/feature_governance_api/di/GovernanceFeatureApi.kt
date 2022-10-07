package io.novafoundation.nova.feature_governance_api.di

import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor

interface GovernanceFeatureApi {

    val governanceSourceRegistry: GovernanceSourceRegistry

    val referendaListInteractor: ReferendaListInteractor

    val governanceUpdateSystem: UpdateSystem
}
