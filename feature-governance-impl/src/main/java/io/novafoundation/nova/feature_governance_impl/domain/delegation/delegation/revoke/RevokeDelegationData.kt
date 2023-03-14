package io.novafoundation.nova.feature_governance_impl.domain.delegation.delegation.revoke

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.domain.delegation.delegate.delegators.model.Delegator
import io.novafoundation.nova.feature_governance_api.domain.track.Track
import kotlin.time.Duration

class RevokeDelegationData(
    val undelegatingPeriod: Duration,
    val delegationsOverview: Delegator.Vote?,
    val delegations: Map<Track, Voting.Delegating>
)
