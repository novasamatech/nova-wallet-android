package io.novafoundation.nova.feature_gift_impl.data

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.core_db.model.GiftLocal
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import kotlinx.coroutines.flow.Flow

interface GiftsRepository {
    fun observeGifts(): Flow<List<Gift>>
}

class RealGiftsRepository(
    private val giftsDao: GiftsDao
) : GiftsRepository {

    override fun observeGifts(): Flow<List<Gift>> {
        return giftsDao.observeAllGifts()
            .mapList { it.toDomain() }
    }

    private fun GiftLocal.toDomain() = Gift(
        id = id,
        chainId = chainId,
        assetId = assetId,
        amount = amount,
        status = when (status) {
            GiftLocal.Status.PENDING -> Gift.Status.PENDING
            GiftLocal.Status.CLAIMED -> Gift.Status.CLAIMED
            GiftLocal.Status.RECLAIMED -> Gift.Status.RECLAIMED
        }
    )
}
