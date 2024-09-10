package io.novafoundation.nova.feature_governance_impl.data.repository.tindergov

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.TinderGovDao
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal.VoteType as LocalVoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import kotlinx.coroutines.flow.Flow

interface TinderGovBasketRepository {

    suspend fun add(item: TinderGovBasketItem)

    fun observeBasket(metaId: Long, chainId: String): Flow<List<TinderGovBasketItem>>
}

class RealTinderGovBasketRepository(private val dao: TinderGovDao) : TinderGovBasketRepository {

    override suspend fun add(item: TinderGovBasketItem) {
        dao.addToBasket(item.toLocal())
    }

    override fun observeBasket(metaId: Long, chainId: String): Flow<List<TinderGovBasketItem>> {
        return dao.observeBasket(metaId, chainId)
            .mapList { it.toDomain() }
    }

    private fun TinderGovBasketItem.toLocal(): TinderGovBasketItemLocal {
        return TinderGovBasketItemLocal(
            metaId = this.metaId,
            chainId = this.chainId,
            referendumId = this.referendumId.value,
            voteType = this.voteTypeToLocal(),
            conviction = this.conviction.toLocal(),
            amount = this.amount
        )
    }

    private fun TinderGovBasketItem.voteTypeToLocal() = when (this.vote) {
        VoteType.AYE -> LocalVoteType.AYE
        VoteType.NAY -> LocalVoteType.NAY
        VoteType.ABSTAIN -> LocalVoteType.ABSTAIN
    }

    private fun TinderGovBasketItemLocal.toDomain(): TinderGovBasketItem {
        return TinderGovBasketItem(
            metaId = this.metaId,
            chainId = this.chainId,
            referendumId = ReferendumId(this.referendumId),
            vote = this.voteTypeToDomain(),
            conviction = this.conviction.toDomain(),
            amount = this.amount
        )
    }

    private fun TinderGovBasketItemLocal.voteTypeToDomain() = when (this.voteType) {
        LocalVoteType.AYE -> VoteType.AYE
        LocalVoteType.NAY -> VoteType.NAY
        LocalVoteType.ABSTAIN -> VoteType.ABSTAIN
    }
}
