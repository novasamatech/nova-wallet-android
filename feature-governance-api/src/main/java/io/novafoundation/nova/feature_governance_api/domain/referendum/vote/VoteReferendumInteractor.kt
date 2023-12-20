package io.novafoundation.nova.feature_governance_api.domain.referendum.vote

import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.AccountVote
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface VoteReferendumInteractor {

    fun voteAssistantFlow(
        referendumId: ReferendumId,
        scope: CoroutineScope
    ): Flow<GovernanceVoteAssistant>

    suspend fun estimateFee(amount: Balance, conviction: Conviction, referendumId: ReferendumId): Fee

    suspend fun vote(
        vote: AccountVote.Standard,
        referendumId: ReferendumId,
    ): Result<ExtrinsicSubmission>
}
