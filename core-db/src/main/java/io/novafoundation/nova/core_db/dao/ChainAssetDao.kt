package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal

typealias FullAssetIdLocal = Pair<String, Int>

@Dao
abstract class ChainAssetDao {

    @Transaction
    open suspend fun updateAssets(diff: CollectionDiffer.Diff<ChainAssetLocal>) {
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

    @Query("SELECT * FROM chain_assets WHERE enabled=1")
    abstract suspend fun getEnabledAssets(): List<ChainAssetLocal>

    @Update(entity = ChainAssetLocal::class)
    abstract suspend fun setAssetsEnabled(params: List<SetAssetEnabledParams>)

    @Delete
    protected abstract suspend fun deleteChainAssets(assets: List<ChainAssetLocal>)
}

class SetAssetEnabledParams(val enabled: Boolean, val chainId: String, val id: Int)
