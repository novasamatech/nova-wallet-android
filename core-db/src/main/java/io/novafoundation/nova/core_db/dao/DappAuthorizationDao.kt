package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.DappAuthorizationLocal

@Dao
interface DappAuthorizationDao {

    @Query("SELECT * FROM dapp_authorizations WHERE baseUrl = :baseUrl")
    suspend fun getAuthorization(baseUrl: String): DappAuthorizationLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAuthorization(dappAuthorization: DappAuthorizationLocal)
}
