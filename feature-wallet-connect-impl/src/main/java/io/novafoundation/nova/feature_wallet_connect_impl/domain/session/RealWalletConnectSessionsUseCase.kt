package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class RealWalletConnectSessionsUseCase(
    private val sessionRepository: WalletConnectSessionRepository,
) : WalletConnectSessionsUseCase {

    override fun activeSessionsNumberFlow(): Flow<Int> {
        return sessionRepository.numberOfSessionAccountsFlow()
    }

    override fun activeSessionsNumberFlow(metaAccount: MetaAccount): Flow<Int> {
        return sessionRepository.numberOfSessionAccountsFlow(metaAccount)
    }

    override suspend fun activeSessionsNumber(): Int {
        return sessionRepository.numberOfSessionAccounts()
    }

    override suspend fun syncActiveSessions() = withContext(Dispatchers.Default) {
        val activeSessionTopics = Web3Wallet.getListOfActiveSessions().map { it.topic }

        sessionRepository.removeAllSessionsOtherThan(activeSessionTopics)
    }
}
