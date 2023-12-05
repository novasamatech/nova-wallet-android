package io.novafoundation.nova.feature_wallet_connect_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.core_db.model.WalletConnectPairingLocal
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectPairingAccount
import kotlinx.coroutines.flow.Flow

interface WalletConnectPairingRepository {

    suspend fun addPairingAccount(pairingAccount: WalletConnectPairingAccount)

    suspend fun getPairingAccount(pairingTopic: String): WalletConnectPairingAccount?

    fun allPairingAccountsFlow(): Flow<List<WalletConnectPairingAccount>>

    fun pairingAccountsByMetaIdFlow(metaId: Long): Flow<List<WalletConnectPairingAccount>>

    suspend fun removeAllPairingsOtherThan(activePairingTopics: List<String>)
}

class RealWalletConnectPairingRepository(
    private val dao: WalletConnectSessionsDao,
) : WalletConnectPairingRepository {

    override suspend fun addPairingAccount(pairingAccount: WalletConnectPairingAccount) {
        dao.insertPairing(mapSessionToLocal(pairingAccount))
    }

    override suspend fun getPairingAccount(pairingTopic: String): WalletConnectPairingAccount? {
        return dao.getPairing(pairingTopic)?.let(::mapSessionFromLocal)
    }

    override fun allPairingAccountsFlow(): Flow<List<WalletConnectPairingAccount>> {
        return dao.allPairingsFlow().mapList(::mapSessionFromLocal)
    }

    override fun pairingAccountsByMetaIdFlow(metaId: Long): Flow<List<WalletConnectPairingAccount>> {
        return dao.pairingsByMetaIdFlow(metaId).mapList(::mapSessionFromLocal)
    }

    override suspend fun removeAllPairingsOtherThan(activePairingTopics: List<String>) {
        dao.removeAllPairingsOtherThan(activePairingTopics)
    }

    private fun mapSessionToLocal(session: WalletConnectPairingAccount): WalletConnectPairingLocal {
        return with(session) {
            WalletConnectPairingLocal(pairingTopic = pairingTopic, metaId = metaId)
        }
    }

    private fun mapSessionFromLocal(sessionLocal: WalletConnectPairingLocal): WalletConnectPairingAccount {
        return with(sessionLocal) {
            WalletConnectPairingAccount(pairingTopic = pairingTopic, metaId = metaId)
        }
    }
}
