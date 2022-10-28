package io.novafoundation.nova.feature_governance_impl.data.source

import io.novafoundation.nova.feature_governance_api.data.repository.ConvictionVotingRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OffChainReferendaInfoRepository
import io.novafoundation.nova.feature_governance_api.data.repository.OnChainReferendaRepository
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource

internal class StaticGovernanceSource(
    override val referenda: OnChainReferendaRepository,
    override val convictionVoting: ConvictionVotingRepository,
    override val offChainInfo: OffChainReferendaInfoRepository
) : GovernanceSource
