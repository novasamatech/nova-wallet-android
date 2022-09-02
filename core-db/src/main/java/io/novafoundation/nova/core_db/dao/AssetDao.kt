package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.AssetWithToken
import kotlinx.coroutines.flow.Flow

private const val RETRIEVE_ASSET_SQL_META_ID = """
    SELECT * FROM assets AS a 
    INNER JOIN chain_assets AS ca ON a.assetId = ca.id AND a.chainId = ca.chainId
    INNER JOIN currencies as currency ON currency.selected = 1
    LEFT JOIN tokens AS t ON ca.symbol = t.tokenSymbol AND currency.id = t.currencyId
    WHERE a.metaId = :metaId AND a.chainId = :chainId AND a.assetId = :assetId
"""

private const val RETRIEVE_ACCOUNT_ASSETS_QUERY = """
    SELECT * FROM assets AS a 
    INNER JOIN chain_assets AS ca ON a.assetId = ca.id AND a.chainId = ca.chainId
    INNER JOIN currencies as currency ON currency.selected = 1
    LEFT JOIN tokens AS t ON ca.symbol = t.tokenSymbol AND currency.id = t.currencyId
    WHERE a.metaId = :metaId
"""

interface AssetReadOnlyCache {

    fun observeAssets(metaId: Long): Flow<List<AssetWithToken>>

    suspend fun getAssetsWithToken(metaId: Long): List<AssetWithToken>

    fun observeAsset(metaId: Long, chainId: String, assetId: Int): Flow<AssetWithToken?>

    suspend fun getAssetWithToken(metaId: Long, chainId: String, assetId: Int): AssetWithToken?

    suspend fun getAsset(metaId: Long, chainId: String, assetId: Int): AssetLocal?
}

@Dao
abstract class AssetDao : AssetReadOnlyCache {

    @Query(RETRIEVE_ACCOUNT_ASSETS_QUERY)
    abstract override fun observeAssets(metaId: Long): Flow<List<AssetWithToken>>

    @Query(RETRIEVE_ACCOUNT_ASSETS_QUERY)
    abstract override suspend fun getAssetsWithToken(metaId: Long): List<AssetWithToken>

    @Query(RETRIEVE_ASSET_SQL_META_ID)
    abstract override fun observeAsset(metaId: Long, chainId: String, assetId: Int): Flow<AssetWithToken?>

    @Query(RETRIEVE_ASSET_SQL_META_ID)
    abstract override suspend fun getAssetWithToken(metaId: Long, chainId: String, assetId: Int): AssetWithToken?

    @Query("SELECT * FROM assets WHERE metaId = :metaId AND chainId = :chainId AND assetId = :assetId")
    abstract override suspend fun getAsset(metaId: Long, chainId: String, assetId: Int): AssetLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAsset(asset: AssetLocal)
}
