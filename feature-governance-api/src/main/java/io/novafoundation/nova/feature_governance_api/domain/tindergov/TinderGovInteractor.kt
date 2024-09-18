package io.novafoundation.nova.feature_governance_api.domain.tindergov

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

sealed interface VotingPowerState {

    object Empty : VotingPowerState

    class InsufficientAmount(val votingPower: VotingPower) : VotingPowerState

    class SufficientAmount(val votingPower: VotingPower) : VotingPowerState
}

interface TinderGovInteractor {

    fun observeReferendaState(coroutineScope: CoroutineScope): Flow<ReferendaState>

    fun observeReferendaAvailableToVote(coroutineScope: CoroutineScope): Flow<List<ReferendumPreview>>

    fun observeTinderGovBasket(): Flow<List<TinderGovBasketItem>>

    suspend fun getTinderGovBasket(): List<TinderGovBasketItem>

    suspend fun addItemToBasket(referendumId: ReferendumId, voteType: VoteType)

    suspend fun loadReferendumSummary(id: ReferendumId): String?

    suspend fun loadReferendumAmount(referendumPreview: ReferendumPreview): BigInteger?

    suspend fun setVotingPower(votingPower: VotingPower)

    suspend fun getVotingPower(metaId: Long, chainId: ChainId): VotingPower?

    suspend fun getVotingPowerState(): VotingPowerState

    suspend fun removeReferendumFromBasket(item: TinderGovBasketItem)

    suspend fun removeBasketItems(items: Collection<TinderGovBasketItem>)

    suspend fun isBasketEmpty(): Boolean

    suspend fun clearBasket()

    suspend fun getBasketItemsToRemove(coroutineScope: CoroutineScope): List<TinderGovBasketItem>
}
