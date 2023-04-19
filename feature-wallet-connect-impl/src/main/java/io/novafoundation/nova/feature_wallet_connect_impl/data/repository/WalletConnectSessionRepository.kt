package io.novafoundation.nova.feature_wallet_connect_impl.data.repository

import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.core_db.model.WalletConnectSessionLocal
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSession

interface WalletConnectSessionRepository {

    suspend fun addSession(session: WalletConnectSession)

    suspend fun getSession(sessionTopic: String): WalletConnectSession?

    suspend fun deleteSession(sessionTopic: String)
}

class RealWalletConnectSessionRepository(
    private val dao: WalletConnectSessionsDao,
) : WalletConnectSessionRepository {

    override suspend fun addSession(session: WalletConnectSession) {
        dao.insertSession(mapSessionToLocal(session))
    }

    override suspend fun getSession(sessionTopic: String): WalletConnectSession? {
        return dao.getSession(sessionTopic)?.let(::mapSessionFromLocal)
    }

    override suspend fun deleteSession(sessionTopic: String) {
        dao.deleteSession(sessionTopic)
    }

    private fun mapSessionToLocal(session: WalletConnectSession): WalletConnectSessionLocal {
        return with(session) {
            WalletConnectSessionLocal(sessionTopic = sessionTopic, metaId = metaId)
        }
    }

    private fun mapSessionFromLocal(sessionLocal: WalletConnectSessionLocal): WalletConnectSession {
        return with(sessionLocal) {
            WalletConnectSession(sessionTopic = sessionTopic, metaId = metaId)
        }
    }
}
