package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.DappAuthorizationLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface DappAuthorizationDao {

    @Query("SELECT * FROM dapp_authorizations WHERE baseUrl = :baseUrl AND metaId = :metaId")
    suspend fun getAuthorization(baseUrl: String, metaId: Long): DappAuthorizationLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAuthorization(dappAuthorization: DappAuthorizationLocal)

    @Query("UPDATE dapp_authorizations SET authorized = 0 WHERE baseUrl = :baseUrl AND metaId = :metaId")
    suspend fun removeAuthorization(baseUrl: String, metaId: Long)

    @Query("SELECT * FROM dapp_authorizations WHERE metaId = :metaId")
    fun observeAuthorizations(metaId: Long): Flow<List<DappAuthorizationLocal>>
}
