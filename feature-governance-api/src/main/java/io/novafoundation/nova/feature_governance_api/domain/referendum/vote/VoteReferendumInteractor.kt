package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface VoteReferendumInteractor {

    fun voteAssistantFlow(referendumId: ReferendumId, scope: CoroutineScope): Flow<GovernanceVoteAssistant>

    fun voteAssistantFlow(referendaIds: List<ReferendumId>, scope: CoroutineScope): Flow<GovernanceVoteAssistant>

    suspend fun estimateFee(referendumId: ReferendumId, vote: AccountVote): Fee

    suspend fun estimateFee(votes: Map<ReferendumId, AccountVote>): Fee

    suspend fun vote(referendumId: ReferendumId, vote: AccountVote): Result<ExtrinsicSubmission>

    suspend fun vote(votes: Map<ReferendumId, AccountVote>): Result<ExtrinsicSubmission>

    suspend fun isAbstainSupported(): Boolean
}
