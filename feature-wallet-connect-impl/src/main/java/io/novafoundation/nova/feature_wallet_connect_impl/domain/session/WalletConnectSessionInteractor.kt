package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionProposal
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.caip.caip2.identifier.Caip2Namespace
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectSessionRepository
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approveSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejected
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.WalletConnectRequest
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface WalletConnectSessionInteractor {

    suspend fun approveSession(
        sessionProposal: SessionProposal,
        metaAccount: MetaAccount
    ): Result<Unit>

    suspend fun rejectSession(proposal: SessionProposal): Result<Unit>

    suspend fun parseSessionRequest(request: Wallet.Model.SessionRequest): Result<WalletConnectRequest>

    suspend fun onSessionSettled(settledSessionResponse: Wallet.Model.SettledSessionResponse)

    suspend fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete)

    suspend fun getSession(sessionTopic: String): WalletConnectSession?
}

class RealWalletConnectSessionInteractor(
    private val chainRegistry: ChainRegistry,
    private val caip2Resolver: Caip2Resolver,
    private val walletConnectRequestFactory: WalletConnectRequest.Factory,
    private val walletConnectSessionRepository: WalletConnectSessionRepository,
) : WalletConnectSessionInteractor {

    private val pendingSessionSettlementsByPairingTopic = ConcurrentHashMap<String, PendingSessionSettlement>()

    override suspend fun approveSession(
        sessionProposal: SessionProposal,
        metaAccount: MetaAccount
    ): Result<Unit> {
        val requestedNameSpaces = sessionProposal.requiredNamespaces + sessionProposal.optionalNamespaces

        val localChains = chainRegistry.currentChains.first()

        val namespaceSessions = requestedNameSpaces.mapValuesNotNull { (namespaceRaw, namespaceProposal) ->
            // TODO handle https://docs.walletconnect.com/2.0/specs/clients/sign/namespaces#13-chains-might-be-omitted-if-the-caip-2-is-defined-in-the-index
            val namespace = Caip2Namespace.find(namespaceRaw) ?: return@mapValuesNotNull null
            val requestedChains = namespaceProposal.chains ?: return@mapValuesNotNull null

            val chainByCaip2 = localChains.associateBy { chain -> caip2Resolver.caip2Of(chain, preferredNamespace = namespace)?.namespaceWitId }

            val supportedChainsWithAccounts = requestedChains.mapNotNull { requestedChain ->
                val chain = chainByCaip2[requestedChain] ?: return@mapNotNull null
                val address = metaAccount.addressIn(chain) ?: return@mapNotNull null

                formatWalletConnectAccount(address, requestedChain) to requestedChain
            }

            Wallet.Model.Namespace.Session(
                chains = supportedChainsWithAccounts.map { (_, chain) -> chain },
                accounts = supportedChainsWithAccounts.map { (address, _) -> address },
                methods = namespaceProposal.methods,
                events = namespaceProposal.events
            )
        }

        val response = sessionProposal.approved(namespaceSessions)

        return Web3Wallet.approveSession(response)
            .onSuccess { registerPendingSettlement(sessionProposal, metaAccount) }
    }

    override suspend fun rejectSession(proposal: SessionProposal): Result<Unit> {
        val response = proposal.rejected("Rejected by user")

        return Web3Wallet.rejectSession(response)
    }

    override suspend fun parseSessionRequest(request: Wallet.Model.SessionRequest): Result<WalletConnectRequest> = runCatching {
        withContext(Dispatchers.Default) {
            requireNotNull(walletConnectRequestFactory.create(request)) {
                "${request.request.method} is not supported"
            }
        }
    }

    override suspend fun onSessionSettled(settledSessionResponse: Wallet.Model.SettledSessionResponse) {
        if (settledSessionResponse !is Wallet.Model.SettledSessionResponse.Result) return

        val pairingTopic = settledSessionResponse.session.pairingTopic
        val pendingSessionSettlement = pendingSessionSettlementsByPairingTopic[pairingTopic] ?: return

        val sessionTopic = settledSessionResponse.session.topic
        val walletConnectSession = WalletConnectSession(pendingSessionSettlement.metaId, sessionTopic)
        walletConnectSessionRepository.addSession(walletConnectSession)
    }

    override suspend fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        if (sessionDelete !is Wallet.Model.SessionDelete.Success) return

        walletConnectSessionRepository.deleteSession(sessionDelete.topic)
    }

    override suspend fun getSession(sessionTopic: String): WalletConnectSession? {
        return walletConnectSessionRepository.getSession(sessionTopic)
    }

    private fun formatWalletConnectAccount(address: String, chainCaip2: String): String {
        return "$chainCaip2:$address"
    }

    private fun registerPendingSettlement(sessionProposal: SessionProposal, metaAccount: MetaAccount) {
        pendingSessionSettlementsByPairingTopic[sessionProposal.pairingTopic] = PendingSessionSettlement(metaAccount.id)
    }

    private class PendingSessionSettlement(val metaId: Long)
}
