package io.novafoundation.nova.feature_governance_api.domain.tindergov

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumCall
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaState
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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

    suspend fun getReferendumAmount(referendumPreview: ReferendumPreview): ReferendumCall.TreasuryRequest?

    suspend fun setVotingPower(votingPower: VotingPower)

    suspend fun getVotingPower(metaId: Long, chainId: ChainId): VotingPower?

    suspend fun getVotingPowerState(): VotingPowerState

    suspend fun awaitAllItemsVoted(coroutineScope: CoroutineScope, basket: List<TinderGovBasketItem>)
}
