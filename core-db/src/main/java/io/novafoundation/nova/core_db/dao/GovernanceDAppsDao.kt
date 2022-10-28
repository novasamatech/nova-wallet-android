package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import kotlinx.coroutines.flow.Flow

@Dao
abstract class GovernanceDAppsDao {

    @Transaction
    open suspend fun update(newDapps: List<GovernanceDAppLocal>) {
        val oldDapps = getAll()
        val dappDiffs = CollectionDiffer.findDiff(newDapps, oldDapps, false)

        deleteDapps(dappDiffs.removed)
        updateDapps(dappDiffs.updated)
        insertDapps(dappDiffs.added)
    }

    @Query("SELECT * FROM governance_dapps")
    abstract fun getAll(): List<GovernanceDAppLocal>

    @Query("SELECT * FROM governance_dapps WHERE chainId = :chainId")
    abstract fun observeChainDapps(chainId: String): Flow<List<GovernanceDAppLocal>>

    @Delete
    abstract suspend fun deleteDapps(dapps: List<GovernanceDAppLocal>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertDapps(dapps: List<GovernanceDAppLocal>)

    @Update
    abstract suspend fun updateDapps(dapps: List<GovernanceDAppLocal>)
}
