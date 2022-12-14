package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal

typealias FullAssetIdLocal = Pair<String, Int>

@Dao
abstract class ChainAssetDao {

    @Transaction
    open suspend fun updateAssetsBySource(newAssets: List<ChainAssetLocal>, source: AssetSourceLocal) {
        val oldAssets = getAssetsBySource(source)
        val diff = CollectionDiffer.findDiff(newAssets, oldAssets, forceUseNewItems = false)
        insertAssets(diff.newOrUpdated)
        deleteChainAssets(diff.removed)
    }

    @Query("SELECT * FROM chain_assets WHERE id = :id AND chainId = :chainId")
    abstract suspend fun getAsset(id: Int, chainId: String): ChainAssetLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAsset(asset: ChainAssetLocal)

    @Query("SELECT * FROM chain_assets WHERE source = :source")
    abstract suspend fun getAssetsBySource(source: AssetSourceLocal): List<ChainAssetLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAssets(assets: List<ChainAssetLocal>)

    @Query("UPDATE chain_assets SET enabled = :enabled WHERE chainId = :chainId AND id = :assetId")
    protected abstract suspend fun setAssetEnabled(enabled: Boolean, chainId: String, assetId: Int)

    @Query("SELECT * FROM chain_assets")
    abstract suspend fun getAllAssets(): List<ChainAssetLocal>

    @Transaction
    open suspend fun setAssetsEnabled(enabled: Boolean, fullAssetIds: List<FullAssetIdLocal>) {
        fullAssetIds.forEach { (chainId, assetId) ->
            setAssetEnabled(enabled, chainId, assetId)
        }
    }

    @Delete
    protected abstract suspend fun deleteChainAssets(assets: List<ChainAssetLocal>)
}
