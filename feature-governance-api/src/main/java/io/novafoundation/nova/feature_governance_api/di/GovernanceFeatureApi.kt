package io.novafoundation.nova.feature_governance_api.di

import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository

interface GovernanceFeatureApi {

    val onChainReferendaRepository: OnChainReferendaRepository
}
