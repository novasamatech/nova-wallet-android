package io.novafoundation.nova.feature_governance_api.di

import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin

interface GovernanceFeatureApi {

    val assetMixinFactory: MixinFactory<AssetSelectorMixin.Presentation>

    val onChainReferendaRepository: OnChainReferendaRepository

    val convictionVotingRepository: ConvictionVotingRepository
}
