package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal
import io.novafoundation.nova.core_db.model.TinderGovVotingPowerLocal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

@Dao
interface TinderGovDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVotingPower(item: TinderGovVotingPowerLocal)

    @Query("SELECT * FROM tinder_gov_voting_power WHERE metaId = :metaId AND chainId = :chainId")
    suspend fun getVotingPower(metaId: Long, chainId: String): TinderGovVotingPowerLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToBasket(item: TinderGovBasketItemLocal)

    @Delete
    suspend fun removeFromBasket(item: TinderGovBasketItemLocal)

    @Delete
    suspend fun removeFromBasket(items: List<TinderGovBasketItemLocal>)

    @Query("SELECT * FROM tinder_gov_basket WHERE metaId = :metaId AND chainId == :chainId")
    fun observeBasket(metaId: Long, chainId: String): Flow<List<TinderGovBasketItemLocal>>

    @Query("SELECT COUNT(*) FROM tinder_gov_basket")
    fun basketSize(): Int
}
