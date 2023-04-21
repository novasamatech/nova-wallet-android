package io.novafoundation.nova.feature_wallet_connect_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.core_db.model.WalletConnectSessionAccountLocal
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionAccount
import kotlinx.coroutines.flow.Flow

interface WalletConnectSessionRepository {

    suspend fun addSessionAccount(sessionAccount: WalletConnectSessionAccount)

    suspend fun getSessionAccount(sessionTopic: String): WalletConnectSessionAccount?

    suspend fun deleteSessionAccount(sessionTopic: String)

    fun allSessionAccountsFlow(): Flow<List<WalletConnectSessionAccount>>
}

class RealWalletConnectSessionRepository(
    private val dao: WalletConnectSessionsDao,
) : WalletConnectSessionRepository {

    override suspend fun addSessionAccount(sessionAccount: WalletConnectSessionAccount) {
        dao.insertSession(mapSessionToLocal(sessionAccount))
    }

    override suspend fun getSessionAccount(sessionTopic: String): WalletConnectSessionAccount? {
        return dao.getSession(sessionTopic)?.let(::mapSessionFromLocal)
    }

    override suspend fun deleteSessionAccount(sessionTopic: String) {
        dao.deleteSession(sessionTopic)
    }

    override fun allSessionAccountsFlow(): Flow<List<WalletConnectSessionAccount>> {
        return dao.allSessionsFlow().mapList(::mapSessionFromLocal)
    }

    private fun mapSessionToLocal(session: WalletConnectSessionAccount): WalletConnectSessionAccountLocal {
        return with(session) {
            WalletConnectSessionAccountLocal(sessionTopic = sessionTopic, metaId = metaId)
        }
    }

    private fun mapSessionFromLocal(sessionLocal: WalletConnectSessionAccountLocal): WalletConnectSessionAccount {
        return with(sessionLocal) {
            WalletConnectSessionAccount(sessionTopic = sessionTopic, metaId = metaId)
        }
    }
}
