package io.novafoundation.nova.feature_governance_api.domain.tindergov

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface TinderGovInteractor {

    fun observeReferendaAvailableToVote(coroutineScope: CoroutineScope): Flow<List<ReferendumPreview>>

    fun observeTinderGovBasket(): Flow<List<TinderGovBasketItem>>

    suspend fun addItemToBasket(referendumId: ReferendumId, voteType: VoteType)

    suspend fun loadReferendumSummary(id: ReferendumId): String?

    suspend fun loadReferendumAmount(referendumPreview: ReferendumPreview): BigInteger?

    suspend fun setVotingPower(chainId: ChainId, amount: BigInteger, conviction: Conviction)

    suspend fun getVotingPower(chainId: ChainId): VotingPower?

    suspend fun isSufficientAmountToVote(): Boolean
}
