package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.ContributionLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ContributionDao {

    @Transaction
    open suspend fun updateContributions(contributions: CollectionDiffer.Diff<ContributionLocal>) {
        insertContributions(contributions.added)
        updateContributions(contributions.updated)
        deleteContributions(contributions.removed)
    }

    @Query("SELECT * FROM contributions WHERE metaId = :metaId AND chainId = :chainId AND assetId = :assetId")
    abstract fun observeContributions(metaId: Long, chainId: String, assetId: Int): Flow<List<ContributionLocal>>

    @Query("SELECT * FROM contributions WHERE metaId = :metaId")
    abstract fun observeContributions(metaId: Long): Flow<List<ContributionLocal>>

    @Query("SELECT * FROM contributions WHERE metaId = :metaId AND chainId = :chainId AND assetId = :assetId AND sourceId = :sourceId")
    abstract suspend fun getContributions(metaId: Long, chainId: String, assetId: Int, sourceId: String): List<ContributionLocal>

    @Query("DELETE FROM contributions WHERE chainId = :chainId AND assetId = :assetId")
    abstract suspend fun deleteContributions(chainId: String, assetId: Int)

    @Delete
    protected abstract suspend fun deleteContributions(contributions: List<ContributionLocal>)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun updateContributions(contributions: List<ContributionLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertContributions(contributions: List<ContributionLocal>)

    @Delete(entity = ContributionLocal::class)
    abstract suspend fun deleteAssetContributions(params: List<DeleteAssetContributionsParams>)
}

class DeleteAssetContributionsParams(val chainId: String, val assetId: Int)
