package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.novafoundation.nova.core_db.model.GiftLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface GiftsDao {

    @Query("SELECT * from gifts")
    fun observeAllGifts(): Flow<List<GiftLocal>>

    @Query("SELECT * from gifts WHERE chainId = :chainId AND assetId = :assetId")
    fun observeGiftsByAsset(chainId: String, assetId: Int): Flow<List<GiftLocal>>

    @Insert
    suspend fun createNewGift(giftLocal: GiftLocal)
}
