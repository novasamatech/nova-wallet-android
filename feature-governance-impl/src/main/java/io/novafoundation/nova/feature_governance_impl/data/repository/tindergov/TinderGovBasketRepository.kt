package io.novafoundation.nova.feature_governance_impl.data.repository.tindergov

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.TinderGovDao
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal.*
import io.novafoundation.nova.core_db.model.common.ConvictionLocal
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal.VoteType as LocalVoteType
import io.novafoundation.nova.core_db.model.common.ConvictionLocal as LocalConviction
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.custom.vote.Conviction
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
            voteType = when (this.vote) {
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
            vote = when (this.voteType) {
                LocalVoteType.AYE -> VoteType.AYE
                LocalVoteType.NAY -> VoteType.NAY
                LocalVoteType.ABSTAIN -> VoteType.ABSTAIN
            },
            conviction = this.conviction.toDomain(),
            amount = this.amount
        )
    }
}
