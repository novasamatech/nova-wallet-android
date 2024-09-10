package io.novafoundation.nova.feature_governance_impl.data.repository.tindergov

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.TinderGovDao
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal.VoteType as LocalVoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface TinderGovBasketRepository {

    suspend fun add(item: TinderGovBasketItem)

    suspend fun remove(item: TinderGovBasketItem)

    suspend fun remove(items: Collection<TinderGovBasketItem>)

    suspend fun isBasketEmpty(): Boolean

    fun observeBasket(metaId: Long, chainId: String): Flow<List<TinderGovBasketItem>>
}

class RealTinderGovBasketRepository(private val dao: TinderGovDao) : TinderGovBasketRepository {

    override suspend fun add(item: TinderGovBasketItem) {
        dao.addToBasket(item.toLocal())
    }

    override suspend fun remove(item: TinderGovBasketItem) {
        withContext(Dispatchers.Default) { dao.removeFromBasket(item.toLocal()) }
    }

    override suspend fun remove(items: Collection<TinderGovBasketItem>) {
        withContext(Dispatchers.Default) { dao.removeFromBasket(items.map { it.toLocal() }) }
    }

    override fun observeBasket(metaId: Long, chainId: String): Flow<List<TinderGovBasketItem>> {
        return dao.observeBasket(metaId, chainId)
            .mapList { it.toDomain() }
    }

    override suspend fun isBasketEmpty(): Boolean {
        return withContext(Dispatchers.Default) { dao.basketSize() == 0 }
    }

    private fun TinderGovBasketItem.toLocal(): TinderGovBasketItemLocal {
        return TinderGovBasketItemLocal(
            metaId = this.metaId,
            chainId = this.chainId,
            referendumId = this.referendumId.value,
            voteType = when (this.voteType) {
                VoteType.AYE -> LocalVoteType.AYE
                VoteType.NAY -> LocalVoteType.NAY
                VoteType.ABSTAIN -> LocalVoteType.ABSTAIN
            },
            conviction = this.conviction.toLocal(),
            amount = this.amount
        )
    }

    private fun TinderGovBasketItemLocal.toDomain(): TinderGovBasketItem {
        return TinderGovBasketItem(
            metaId = this.metaId,
            chainId = this.chainId,
            referendumId = ReferendumId(this.referendumId),
            voteType = when (this.voteType) {
                LocalVoteType.AYE -> VoteType.AYE
                LocalVoteType.NAY -> VoteType.NAY
                LocalVoteType.ABSTAIN -> VoteType.ABSTAIN
            },
            conviction = this.conviction.toDomain(),
            amount = this.amount
        )
    }
}
