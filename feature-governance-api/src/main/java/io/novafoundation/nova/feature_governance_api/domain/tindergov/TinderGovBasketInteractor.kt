package io.novafoundation.nova.feature_governance_api.domain.tindergov

import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface TinderGovBasketInteractor {

    fun observeTinderGovBasket(): Flow<List<TinderGovBasketItem>>

    suspend fun getTinderGovBasket(): List<TinderGovBasketItem>

    suspend fun addItemToBasket(referendumId: ReferendumId, voteType: VoteType)

    suspend fun removeReferendumFromBasket(item: TinderGovBasketItem)

    suspend fun removeBasketItems(items: Collection<TinderGovBasketItem>)

    suspend fun isBasketEmpty(): Boolean

    suspend fun clearBasket()

    suspend fun getBasketItemsToRemove(coroutineScope: CoroutineScope): List<TinderGovBasketItem>

    suspend fun awaitAllItemsVoted(coroutineScope: CoroutineScope, basket: List<TinderGovBasketItem>)
}
