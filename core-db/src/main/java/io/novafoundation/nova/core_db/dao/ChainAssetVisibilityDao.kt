package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.ChainAssetVisibilityLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface ChainAssetVisibilityDao {

    @Query("SELECT * FROM chain_asset_visibility WHERE metaId = :metaId")
    fun observeVisibility(metaId: Long): Flow<List<ChainAssetVisibilityLocal>>

    @Query("SELECT * FROM chain_asset_visibility WHERE metaId = :metaId")
    suspend fun getVisibility(metaId: Long): List<ChainAssetVisibilityLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setVisibility(visibility: List<ChainAssetVisibilityLocal>)

    /**
     * IGNORE rather than REPLACE: an asset that already has a row has already been decided about,
     * and balance discovery must not overwrite that decision in either direction.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(visibility: List<ChainAssetVisibilityLocal>)
}
