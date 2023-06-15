package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionProposal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionAccount
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionDetails
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionProposal
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.WalletConnectRequest
import kotlinx.coroutines.flow.Flow

interface WalletConnectSessionInteractor {

    suspend fun approveSession(
        sessionProposal: SessionProposal,
        metaAccount: MetaAccount
    ): Result<Unit>

    suspend fun resolveSessionProposal(sessionProposal: SessionProposal): WalletConnectSessionProposal

    suspend fun rejectSession(proposal: SessionProposal): Result<Unit>

    suspend fun parseSessionRequest(request: Wallet.Model.SessionRequest): Result<WalletConnectRequest>

    suspend fun onSessionSettled(settledSessionResponse: Wallet.Model.SettledSessionResponse)

    suspend fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete)

    suspend fun getSessionAccount(sessionTopic: String): WalletConnectSessionAccount?

    fun activeSessionsFlow(metaId: Long): Flow<List<WalletConnectSession>>

    fun activeSessionsFlow(): Flow<List<WalletConnectSession>>

    fun activeSessionFlow(sessionTopic: String): Flow<WalletConnectSessionDetails?>

    suspend fun disconnect(sessionTopic: String): Result<*>
}
