package io.novafoundation.nova.feature_wallet_connect_impl.data.repository

import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.core_db.model.WalletConnectSessionAccountLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface WalletConnectSessionRepository {

    suspend fun addSessionAccount(sessionAccount: WalletConnectSessionAccount)

    suspend fun getSessionAccount(sessionTopic: String): WalletConnectSessionAccount?

    suspend fun deleteSessionAccount(sessionTopic: String)

    fun allSessionAccountsFlow(): Flow<List<WalletConnectSessionAccount>>

    fun sessionAccountsByMetaIdFlow(metaId: Long): Flow<List<WalletConnectSessionAccount>>

    fun numberOfSessionAccountsFlow(): Flow<Int>

    fun numberOfSessionAccountsFlow(metaAccount: MetaAccount): Flow<Int>

    suspend fun numberOfSessionAccounts(): Int

    fun sessionAccountFlow(sessionTopic: String): Flow<WalletConnectSessionAccount?>

    suspend fun removeAllSessionsOtherThan(activeSessionTopics: List<String>)
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

    override fun sessionAccountsByMetaIdFlow(metaId: Long): Flow<List<WalletConnectSessionAccount>> {
        return dao.sessionsByMetaIdFlow(metaId).mapList(::mapSessionFromLocal)
    }

    override fun numberOfSessionAccountsFlow(): Flow<Int> {
        return dao.numberOfSessionsFlow()
    }

    override fun numberOfSessionAccountsFlow(metaAccount: MetaAccount): Flow<Int> {
        return dao.numberOfSessionsFlow(metaAccount.id)
    }

    override suspend fun numberOfSessionAccounts(): Int {
        return dao.numberOfSessions()
    }

    override fun sessionAccountFlow(sessionTopic: String): Flow<WalletConnectSessionAccount?> {
        return dao.sessionFlow(sessionTopic).map { localSession -> localSession?.let(::mapSessionFromLocal) }
    }

    override suspend fun removeAllSessionsOtherThan(activeSessionTopics: List<String>) {
        dao.removeAllSessionsOtherThan(activeSessionTopics)
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
