package jp.co.soramitsu.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import jp.co.soramitsu.core_db.model.AssetLocal
import jp.co.soramitsu.core_db.model.AssetWithToken
import kotlinx.coroutines.flow.Flow

private const val RETRIEVE_ASSET_SQL_META_ID = """
           select * from assets as a inner join tokens as t ON a.tokenSymbol = t.symbol WHERE
            a.metaId = :metaId and a.chainId = :chainId AND a.tokenSymbol = :symbol
"""

private const val RETRIEVE_ACCOUNT_ASSETS_QUERY = """
       select * from assets as a inner join tokens as t on a.tokenSymbol = t.symbol WHERE a.metaId = :metaId
"""

interface AssetReadOnlyCache {

    fun observeAssets(metaId: Long): Flow<List<AssetWithToken>>
    suspend fun getAssets(metaId: Long): List<AssetWithToken>

    fun observeAsset(metaId: Long, chainId: String, symbol: String): Flow<AssetWithToken?>

    suspend fun getAsset(metaId: Long, chainId: String, symbol: String): AssetWithToken?
}

@Dao
abstract class AssetDao : AssetReadOnlyCache {

    @Query(RETRIEVE_ACCOUNT_ASSETS_QUERY)
    abstract override fun observeAssets(metaId: Long): Flow<List<AssetWithToken>>

    @Query(RETRIEVE_ACCOUNT_ASSETS_QUERY)
    abstract override suspend fun getAssets(metaId: Long): List<AssetWithToken>

    @Query(RETRIEVE_ASSET_SQL_META_ID)
    abstract override fun observeAsset(metaId: Long, chainId: String, symbol: String): Flow<AssetWithToken?>

    @Query(RETRIEVE_ASSET_SQL_META_ID)
    abstract override suspend fun getAsset(metaId: Long, chainId: String, symbol: String): AssetWithToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAsset(asset: AssetLocal)
}
