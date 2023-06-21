package io.novafoundation.nova.feature_wallet_connect_api.domain.sessions

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import kotlinx.coroutines.flow.Flow

interface WalletConnectSessionsUseCase {

    fun activeSessionsNumberFlow(): Flow<Int>

    fun activeSessionsNumberFlow(metaAccount: MetaAccount): Flow<Int>

    suspend fun activeSessionsNumber(): Int

    suspend fun syncActiveSessions()
}
