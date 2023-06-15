package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.WalletConnectSessionAccountLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletConnectSessionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WalletConnectSessionAccountLocal)

    @Query("DELETE FROM wallet_connect_sessions WHERE sessionTopic = :sessionTopic")
    suspend fun deleteSession(sessionTopic: String)

    @Query("SELECT * FROM wallet_connect_sessions WHERE sessionTopic = :sessionTopic")
    suspend fun getSession(sessionTopic: String): WalletConnectSessionAccountLocal?

    @Query("SELECT * FROM wallet_connect_sessions WHERE sessionTopic = :sessionTopic")
    fun sessionFlow(sessionTopic: String): Flow<WalletConnectSessionAccountLocal?>

    @Query("DELETE FROM wallet_connect_sessions WHERE sessionTopic NOT IN (:sessionTopics)")
    suspend fun removeAllSessionsOtherThan(sessionTopics: List<String>)

    @Query("SELECT * FROM wallet_connect_sessions")
    fun allSessionsFlow(): Flow<List<WalletConnectSessionAccountLocal>>

    @Query("SELECT * FROM wallet_connect_sessions WHERE metaId = :metaId")
    fun sessionsByMetaIdFlow(metaId: Long): Flow<List<WalletConnectSessionAccountLocal>>

    @Query("SELECT COUNT(*) FROM wallet_connect_sessions")
    fun numberOfSessionsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM wallet_connect_sessions WHERE metaId = :metaId")
    fun numberOfSessionsFlow(metaId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM wallet_connect_sessions")
    suspend fun numberOfSessions(): Int
}
