package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.core_db.model.AssetAndChainId
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.AssetWithToken
import kotlinx.coroutines.flow.Flow

private const val RETRIEVE_ASSET_SQL_META_ID = """
    SELECT *, ca.chainId as ca_chainId, ca.id as ca_assetId FROM chain_assets AS ca
    LEFT JOIN assets AS a ON a.assetId = ca.id AND a.chainId = ca.chainId AND a.metaId = :metaId
    INNER JOIN currencies as currency ON currency.selected = 1
    LEFT JOIN tokens AS t ON ca.symbol = t.tokenSymbol AND currency.id = t.currencyId
    WHERE ca.chainId = :chainId AND ca.id = :assetId
"""

private const val RETRIEVE_SYNCED_ACCOUNT_ASSETS_QUERY = """
    SELECT *, ca.chainId as ca_chainId, ca.id as ca_assetId FROM assets AS a 
    INNER JOIN chain_assets AS ca ON a.assetId = ca.id AND a.chainId = ca.chainId
    INNER JOIN currencies as currency ON currency.selected = 1
    LEFT JOIN tokens AS t ON ca.symbol = t.tokenSymbol AND currency.id = t.currencyId
    WHERE a.metaId = :metaId
"""

private const val RETRIEVE_SUPPORTED_ACCOUNT_ASSETS_QUERY = """
    SELECT *, ca.chainId as ca_chainId, ca.id as ca_assetId FROM chain_assets AS ca
    LEFT JOIN assets AS a ON a.assetId = ca.id AND a.chainId = ca.chainId AND a.metaId = :metaId
    INNER JOIN currencies as currency ON currency.selected = 1
    LEFT JOIN tokens AS t ON ca.symbol = t.tokenSymbol AND currency.id = t.currencyId
"""

private const val RETRIEVE_ASSETS_SQL_META_ID = """
    SELECT *, ca.chainId as ca_chainId, ca.id as ca_assetId FROM chain_assets AS ca
    LEFT JOIN assets AS a ON a.assetId = ca.id AND a.chainId = ca.chainId AND a.metaId = :metaId
    INNER JOIN currencies as currency ON currency.selected = 1
    LEFT JOIN tokens AS t ON ca.symbol = t.tokenSymbol AND currency.id = t.currencyId
    WHERE ca.chainId || ':' || ca.id in (:joinedChainAndAssetIds)
"""


interface AssetReadOnlyCache {

    fun observeSyncedAssets(metaId: Long): Flow<List<AssetWithToken>>

    suspend fun getSyncedAssets(metaId: Long): List<AssetWithToken>

    fun observeSupportedAssets(metaId: Long): Flow<List<AssetWithToken>>

    suspend fun getSupportedAssets(metaId: Long): List<AssetWithToken>

    fun observeAsset(metaId: Long, chainId: String, assetId: Int): Flow<AssetWithToken>

    fun observeAssets(metaId: Long, assetIds: Collection<AssetAndChainId>): Flow<List<AssetWithToken>>

    suspend fun getAssetWithToken(metaId: Long, chainId: String, assetId: Int): AssetWithToken?

    suspend fun getAsset(metaId: Long, chainId: String, assetId: Int): AssetLocal?

    suspend fun getAssetsInChain(metaId: Long, chainId: String): List<AssetLocal>
}

@Dao
abstract class AssetDao : AssetReadOnlyCache {

    @Query(RETRIEVE_SYNCED_ACCOUNT_ASSETS_QUERY)
    abstract override fun observeSyncedAssets(metaId: Long): Flow<List<AssetWithToken>>

    @Query(RETRIEVE_SYNCED_ACCOUNT_ASSETS_QUERY)
    abstract override suspend fun getSyncedAssets(metaId: Long): List<AssetWithToken>

    @Query(RETRIEVE_SUPPORTED_ACCOUNT_ASSETS_QUERY)
    abstract override fun observeSupportedAssets(metaId: Long): Flow<List<AssetWithToken>>

    @Query(RETRIEVE_SUPPORTED_ACCOUNT_ASSETS_QUERY)
    abstract override suspend fun getSupportedAssets(metaId: Long): List<AssetWithToken>

    @Query(RETRIEVE_ASSET_SQL_META_ID)
    abstract override fun observeAsset(metaId: Long, chainId: String, assetId: Int): Flow<AssetWithToken>

    @Query(RETRIEVE_ASSET_SQL_META_ID)
    abstract override suspend fun getAssetWithToken(metaId: Long, chainId: String, assetId: Int): AssetWithToken?

    @Query("SELECT * FROM assets WHERE metaId = :metaId AND chainId = :chainId AND assetId = :assetId")
    abstract override suspend fun getAsset(metaId: Long, chainId: String, assetId: Int): AssetLocal?

    @Query("SELECT * FROM assets WHERE metaId = :metaId AND chainId = :chainId")
    abstract override suspend fun getAssetsInChain(metaId: Long, chainId: String): List<AssetLocal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAsset(asset: AssetLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAssets(assets: List<AssetLocal>)

    @Delete(entity = AssetLocal::class)
    abstract suspend fun clearAssets(assetIds: List<ClearAssetsParams>)

    @Query(RETRIEVE_ASSETS_SQL_META_ID)
    protected abstract fun observeJoinedAssets(metaId: Long, joinedChainAndAssetIds: Set<String>): Flow<List<AssetWithToken>>

    override fun observeAssets(metaId: Long, assetIds: Collection<AssetAndChainId>): Flow<List<AssetWithToken>> {
       return flowOfAll {
           val joinedChainAndAssetIds = assetIds.mapToSet { (chainId, assetId) -> "${chainId}:${assetId}" }

           observeJoinedAssets(metaId, joinedChainAndAssetIds)
       }
    }
}

class ClearAssetsParams(val chainId: String, val assetId: Int)
