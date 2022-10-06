package io.novafoundation.nova.feature_governance_api.data.source

import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository

interface GovernanceSource {

    val referenda: OnChainReferendaRepository

    val convictionVoting: ConvictionVotingRepository
}
