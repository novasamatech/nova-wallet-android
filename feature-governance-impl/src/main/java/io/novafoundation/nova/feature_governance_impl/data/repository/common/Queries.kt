package io.novafoundation.nova.feature_governance_impl.data.repository.common

import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumVoter
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.Voting
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.votes
import io.novafoundation.nova.runtime.storage.source.query.StorageKeyComponents
import io.novasama.substrate_sdk_android.runtime.AccountId

fun Map<StorageKeyComponents, Voting?>.votersFor(referendumId: ReferendumId): List<ReferendumVoter> {
    return mapNotNull { (keyComponents, voting) ->
        val voterId = keyComponents.component1<AccountId>()
        val votes = voting?.votes()

        votes?.get(referendumId)?.let { accountVote ->
            ReferendumVoter(
                accountId = voterId,
                vote = accountVote,
                delegators = emptyList()
            )
        }
    }
}
