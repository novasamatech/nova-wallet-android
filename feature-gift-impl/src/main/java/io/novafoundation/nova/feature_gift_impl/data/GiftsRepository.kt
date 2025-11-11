package io.novafoundation.nova.feature_gift_impl.data

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.GiftsDao
import io.novafoundation.nova.core_db.model.GiftLocal
import io.novafoundation.nova.feature_gift_impl.domain.models.Gift
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GiftsRepository {

    suspend fun getGift(giftId: Long): Gift

    suspend fun getGifts(): List<Gift>

    fun observeGift(giftId: Long): Flow<Gift>

    fun observeGifts(): Flow<List<Gift>>

    suspend fun saveNewGift(accountIdKey: AccountIdKey, amount: BigInteger, fullChainAssetId: FullChainAssetId): Long

    suspend fun setGiftState(id: Long, status: Gift.Status)
}

class RealGiftsRepository(
    private val giftsDao: GiftsDao
) : GiftsRepository {

    override suspend fun getGift(giftId: Long): Gift {
        return giftsDao.getGiftById(giftId).toDomain()
    }

    override suspend fun getGifts(): List<Gift> {
        return giftsDao.getAllGifts().map { it.toDomain() }
    }

    override fun observeGift(giftId: Long): Flow<Gift> {
        return giftsDao.observeGiftById(giftId)
            .map { it.toDomain() }
    }

    override fun observeGifts(): Flow<List<Gift>> {
        return giftsDao.observeAllGifts()
            .mapList { it.toDomain() }
    }

    override suspend fun saveNewGift(
        accountIdKey: AccountIdKey,
        amount: BigInteger,
        fullChainAssetId: FullChainAssetId
    ): Long {
        return giftsDao.createNewGift(
            GiftLocal(
                amount = amount,
                giftAccountId = accountIdKey.value,
                chainId = fullChainAssetId.chainId,
                assetId = fullChainAssetId.assetId,
                status = GiftLocal.Status.PENDING,
                creationDate = Date().time
            )
        )
    }

    override suspend fun setGiftState(id: Long, status: Gift.Status) {
        giftsDao.setGiftState(id, status.toDomain())
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
        },
        creationDate = Date(this.creationDate)
    )

    private fun Gift.Status.toDomain(): GiftLocal.Status {
        return when (this) {
            Gift.Status.PENDING -> GiftLocal.Status.PENDING
            Gift.Status.CLAIMED -> GiftLocal.Status.CLAIMED
            Gift.Status.RECLAIMED -> GiftLocal.Status.RECLAIMED
        }
    }
}
