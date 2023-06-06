package io.novafoundation.nova.feature_wallet_connect_api.domain.sessions

import kotlinx.coroutines.flow.Flow

interface WalletConnectSessionsUseCase {

    fun activeSessionsNumberFlow(): Flow<Int>

    suspend fun activeSessionsNumber(): Int

    suspend fun syncActiveSessions()
}
