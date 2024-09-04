package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.BalanceHoldLocal
import io.novafoundation.nova.core_db.model.TinderGovBasketItemLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TinderGovDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: TinderGovBasketItemLocal)

    @Query("SELECT * FROM tinder_gov_basket WHERE metaId = :metaId AND chainId == :chainId")
    abstract fun observeHoldsForMetaAccount(metaId: Long, chainId: String): Flow<List<TinderGovBasketItemLocal>>
}
