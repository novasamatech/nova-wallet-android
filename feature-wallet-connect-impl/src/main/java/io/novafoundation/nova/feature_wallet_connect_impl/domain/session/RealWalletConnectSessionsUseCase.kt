package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.walletconnect.android.CoreClient
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectPairingRepository
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

internal class RealWalletConnectSessionsUseCase(
    private val pairingRepository: WalletConnectPairingRepository,
    private val sessionRepository: WalletConnectSessionRepository,
) : WalletConnectSessionsUseCase {

    override fun activeSessionsNumberFlow(): Flow<Int> {
        return sessionRepository.numberOfSessionsFlow()
    }

    override fun activeSessionsNumberFlow(metaAccount: MetaAccount): Flow<Int> {
        return pairingRepository.pairingAccountsByMetaIdFlow(metaAccount.id).flatMapLatest { pairings ->
            val pairingTopics = pairings.mapToSet { it.pairingTopic }
            sessionRepository.numberOfSessionsFlow(pairingTopics)
        }
    }

    override suspend fun activeSessionsNumber(): Int {
        return sessionRepository.numberOfSessionAccounts()
    }

    override suspend fun syncActiveSessions() = withContext(Dispatchers.Default) {
        val activePairingTopics = CoreClient.Pairing.getPairings()
            .filter { it.isActive }
            .map { it.topic }

        pairingRepository.removeAllPairingsOtherThan(activePairingTopics)

        val activeSessionTopics = Web3Wallet.getListOfActiveSessions()
        sessionRepository.setSessions(activeSessionTopics)
    }
}
