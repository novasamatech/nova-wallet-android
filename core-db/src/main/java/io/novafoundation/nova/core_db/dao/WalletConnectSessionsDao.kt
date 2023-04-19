package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.WalletConnectSessionLocal

@Dao
interface WalletConnectSessionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WalletConnectSessionLocal)

    @Query("DELETE FROM wallet_connect_sessions WHERE sessionTopic = :sessionTopic")
    suspend fun deleteSession(sessionTopic: String)

    @Query("SELECT * FROM wallet_connect_sessions WHERE sessionTopic = :sessionTopic")
    suspend fun getSession(sessionTopic: String): WalletConnectSessionLocal?
}
