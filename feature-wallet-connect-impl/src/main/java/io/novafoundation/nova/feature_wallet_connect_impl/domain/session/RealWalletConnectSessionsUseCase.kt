package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectSessionRepository
import kotlinx.coroutines.flow.Flow

internal class RealWalletConnectSessionsUseCase(
    private val sessionRepository: WalletConnectSessionRepository,
): WalletConnectSessionsUseCase {

    override fun activeSessionsNumberFlow(): Flow<Int> {
        return sessionRepository.numberOfSessionAccountsFlow()
    }
}
