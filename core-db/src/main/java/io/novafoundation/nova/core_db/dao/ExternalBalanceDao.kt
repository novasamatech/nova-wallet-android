package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.AggregatedExternalBalanceLocal
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import jp.co.soramitsu.fearless_utils.hash.isPositive
import kotlinx.coroutines.flow.Flow

@Dao
interface ExternalBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExternalBalance(externalBalance: ExternalBalanceLocal)

    @Query("DELETE FROM externalBalances WHERE metaId = :metaId AND chainId = :chainId AND assetId = :assetId AND type = :type AND subtype = :subtype")
    suspend fun removeExternalBalance(
        metaId: Long,
        chainId: String,
        assetId: Int,
        type: ExternalBalanceLocal.Type,
        subtype: String?,
    )

    @Delete(entity = ExternalBalanceLocal::class)
    suspend fun deleteAssetExternalBalances(params: List<ExternalBalanceAssetDeleteParams>)

    @Query(
        """
            SELECT chainId, assetId, type, SUM(amount) as aggregatedAmount 
            FROM externalBalances
            WHERE metaId = :metaId
            GROUP BY type
        """
    )
    fun observeAggregatedExternalBalances(metaId: Long): Flow<List<AggregatedExternalBalanceLocal>>

    @Query(
        """
            SELECT chainId, assetId, type, SUM(amount) as aggregatedAmount 
            FROM externalBalances
            WHERE metaId = :metaId AND chainId = :chainId AND assetId = :assetId
            GROUP BY type
        """
    )
    fun observeChainAggregatedExternalBalances(metaId: Long, chainId: String, assetId: Int): Flow<List<AggregatedExternalBalanceLocal>>
}

suspend fun ExternalBalanceDao.updateExternalBalance(externalBalance: ExternalBalanceLocal) {
    if (externalBalance.amount.isPositive()) {
        insertExternalBalance(externalBalance)
    } else {
        removeExternalBalance(
            metaId = externalBalance.metaId,
            chainId = externalBalance.chainId,
            assetId = externalBalance.assetId,
            type = externalBalance.type,
            subtype = externalBalance.subtype
        )
    }
}

class ExternalBalanceAssetDeleteParams(val chainId: String, val assetId: Int)
