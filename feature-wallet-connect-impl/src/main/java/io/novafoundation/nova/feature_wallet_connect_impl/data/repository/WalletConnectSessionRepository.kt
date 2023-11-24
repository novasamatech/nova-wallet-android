package io.novafoundation.nova.feature_wallet_connect_impl.data.repository

import com.walletconnect.web3.wallet.client.Wallet.Model.Session
import io.novafoundation.nova.common.utils.added
import io.novafoundation.nova.common.utils.removed
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface WalletConnectSessionRepository {

    suspend fun setSessions(sessions: List<Session>)

    suspend fun getSession(sessionTopic: String): Session?

    fun allSessionsFlow(): Flow<List<Session>>

    fun sessionFlow(sessionTopic: String): Flow<Session?>

    suspend fun addSession(session: Session)

    suspend fun removeSession(sessionTopic: String)

    fun numberOfSessionsFlow(): Flow<Int>

    suspend fun numberOfSessionAccounts(): Int

    fun numberOfSessionsFlow(pairingTopics: Set<String>): Flow<Int>
}

class InMemoryWalletConnectSessionRepository: WalletConnectSessionRepository {

    private val state = singleReplaySharedFlow<List<Session>>()

    override suspend fun setSessions(sessions: List<Session>) {
        state.emit(sessions)
    }

    override suspend fun getSession(sessionTopic: String): Session? {
        return state.first().find { it.topic == sessionTopic }
    }

    override fun allSessionsFlow(): Flow<List<Session>> {
        return state
    }

    override fun sessionFlow(sessionTopic: String): Flow<Session?> {
        return state.map { allSessions -> allSessions.find { it.topic == sessionTopic } }
    }

    override suspend fun addSession(session: Session) {
        modifyState { current ->
            current.added(session)
        }
    }

    override suspend fun removeSession(sessionTopic: String) {
        modifyState { current ->
            current.removed { it.topic == sessionTopic }
        }
    }

    override fun numberOfSessionsFlow(): Flow<Int> {
        return state.map { it.size }
    }

    override fun numberOfSessionsFlow(pairingTopics: Set<String>): Flow<Int> {
        return state.map {  sessions ->
            sessions.filter { it.pairingTopic in pairingTopics }.size
        }
    }

    override suspend fun numberOfSessionAccounts(): Int {
        return state.first().size
    }

    private suspend fun modifyState(modify: (List<Session>) -> List<Session>) {
        state.emit(modify(state.first()))
    }
}
