package io.novafoundation.nova.core_db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.novafoundation.nova.core_db.model.WalletConnectPairingLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletConnectSessionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPairing(pairing: WalletConnectPairingLocal)

    @Query("DELETE FROM wallet_connect_pairings WHERE pairingTopic = :pairingTopic")
    suspend fun deletePairing(pairingTopic: String)

    @Query("SELECT * FROM wallet_connect_pairings WHERE pairingTopic = :pairingTopic")
    suspend fun getPairing(pairingTopic: String): WalletConnectPairingLocal?

    @Query("SELECT * FROM wallet_connect_pairings WHERE pairingTopic = :pairingTopic")
    fun pairingFlow(pairingTopic: String): Flow<WalletConnectPairingLocal?>

    @Query("DELETE FROM wallet_connect_pairings WHERE pairingTopic NOT IN (:pairingTopics)")
    suspend fun removeAllPairingsOtherThan(pairingTopics: List<String>)

    @Query("SELECT * FROM wallet_connect_pairings")
    fun allPairingsFlow(): Flow<List<WalletConnectPairingLocal>>

    @Query("SELECT * FROM wallet_connect_pairings WHERE metaId = :metaId")
    fun pairingsByMetaIdFlow(metaId: Long): Flow<List<WalletConnectPairingLocal>>
}
