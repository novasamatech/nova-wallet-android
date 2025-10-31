package io.novafoundation.nova.feature_gift_impl.data

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.core_db.model.GiftLocal
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

interface GiftsRepository {
    fun observeGifts(): Flow<List<Gift>>

    suspend fun saveNewGift(accountIdKey: AccountIdKey, amount: BigInteger, fullChainAssetId: FullChainAssetId)
}

class RealGiftsRepository(
    private val giftsDao: GiftsDao
) : GiftsRepository {

    override fun observeGifts(): Flow<List<Gift>> {
        return giftsDao.observeAllGifts()
            .mapList { it.toDomain() }
    }

    override suspend fun saveNewGift(
        accountIdKey: AccountIdKey,
        amount: BigInteger,
        fullChainAssetId: FullChainAssetId
    ) {
        giftsDao.createNewGift(
            GiftLocal(
                amount = amount,
                giftAccountId = accountIdKey.value,
                chainId = fullChainAssetId.chainId,
                assetId = fullChainAssetId.assetId,
                status = GiftLocal.Status.PENDING
            )
        )
    }

    private fun GiftLocal.toDomain() = Gift(
        id = id,
        chainId = chainId,
        giftAccountId = giftAccountId,
        assetId = assetId,
        amount = amount,
        status = when (status) {
            GiftLocal.Status.PENDING -> Gift.Status.PENDING
            GiftLocal.Status.CLAIMED -> Gift.Status.CLAIMED
            GiftLocal.Status.RECLAIMED -> Gift.Status.RECLAIMED
        }
    )
}
