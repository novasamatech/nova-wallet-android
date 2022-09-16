package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.ContributionLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ContributionDao {

    @Query("SELECT * FROM contributions WHERE metaId = :metaId AND chainId = :chainId")
    abstract fun observeContributions(metaId: Long, chainId: String): Flow<List<ContributionLocal>>

    @Query("SELECT * FROM contributions WHERE metaId = :metaId")
    abstract fun observeContributions(metaId: Long): Flow<List<ContributionLocal>>

    @Query("DELETE FROM contributions WHERE metaId = :metaId AND chainId = :chainId AND type = :type")
    abstract fun deleteByType(metaId: Long, chainId: String, type: ContributionLocal.Type)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(contribution: List<ContributionLocal>)
}
