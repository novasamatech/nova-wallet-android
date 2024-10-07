package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface VoteReferendumInteractor {

    fun voteAssistantFlow(referendumId: ReferendumId, scope: CoroutineScope): Flow<GovernanceVoteAssistant>

    fun voteAssistantFlow(referendaIds: List<ReferendumId>, scope: CoroutineScope): Flow<GovernanceVoteAssistant>

    suspend fun estimateFee(referendumId: ReferendumId, vote: AccountVote): Fee

    suspend fun estimateFee(votes: Map<ReferendumId, AccountVote>): Fee

    suspend fun voteReferendum(referendumId: ReferendumId, vote: AccountVote): Result<ExtrinsicSubmission>

    suspend fun voteReferenda(votes: Map<ReferendumId, AccountVote>): RetriableMultiResult<ExtrinsicStatus.InBlock>

    suspend fun isAbstainSupported(): Boolean
}
