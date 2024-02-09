package io.novafoundation.nova.feature_wallet_connect_impl.domain.session

import com.walletconnect.android.Core
import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.Namespace
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionProposal
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.caip.caip2.Caip2Parser
import io.novafoundation.nova.caip.caip2.Caip2Resolver
import io.novafoundation.nova.caip.caip2.isValidCaip2
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.common.utils.toImmutable
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectPairingRepository
import io.novafoundation.nova.feature_wallet_connect_impl.data.repository.WalletConnectSessionRepository
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.SessionChains
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.SessionDappMetadata
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectPairingAccount
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionDetails
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionDetails.SessionStatus
import io.novafoundation.nova.feature_wallet_connect_impl.domain.model.WalletConnectSessionProposal
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.WalletConnectError
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approveSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.approved
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.disconnectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejectSession
import io.novafoundation.nova.feature_wallet_connect_impl.domain.sdk.rejected
import io.novafoundation.nova.feature_wallet_connect_impl.domain.session.requests.WalletConnectRequest
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private typealias KnownChainsBuilder = MutableSet<Chain>
private typealias UnknownChainsBuilder = MutableSet<String>
private typealias Caip2ChainId = String

class RealWalletConnectSessionInteractor(
    private val caip2Resolver: Caip2Resolver,
    private val caip2Parser: Caip2Parser,
    private val walletConnectRequestFactory: WalletConnectRequest.Factory,
    private val walletConnectPairingRepository: WalletConnectPairingRepository,
    private val walletConnectSessionRepository: WalletConnectSessionRepository,
    private val accountRepository: AccountRepository,
) : WalletConnectSessionInteractor {

    private val pendingSessionSettlementsByPairingTopic = ConcurrentHashMap<String, PendingSessionSettlement>()

    override suspend fun approveSession(
        sessionProposal: SessionProposal,
        metaAccount: MetaAccount
    ): Result<Unit> = withContext(Dispatchers.Default) {
        val requestedNameSpaces = sessionProposal.requiredNamespaces mergeWith sessionProposal.optionalNamespaces

        val chainsByCaip2 = caip2Resolver.chainsByCaip2()

        val namespaceSessions = requestedNameSpaces.mapValuesNotNull { (namespaceRaw, namespaceProposal) ->
            val requestedChains = if (caip2Parser.isValidCaip2(namespaceRaw)) {
                listOf(namespaceRaw)
            } else {
                namespaceProposal.chains
            } ?: return@mapValuesNotNull null

            val supportedChainsWithAccounts = requestedChains.mapNotNull { requestedChain ->
                val chain = chainsByCaip2[requestedChain] ?: return@mapNotNull null
                val address = metaAccount.addressIn(chain) ?: return@mapNotNull null

                formatWalletConnectAccount(address, requestedChain) to requestedChain
            }

            Namespace.Session(
                chains = supportedChainsWithAccounts.map { (_, chain) -> chain },
                accounts = supportedChainsWithAccounts.map { (address, _) -> address },
                methods = namespaceProposal.methods,
                events = namespaceProposal.events
            )
        }

        val response = sessionProposal.approved(namespaceSessions)

        Web3Wallet.approveSession(response)
            .onSuccess { registerPendingSettlement(sessionProposal, metaAccount) }
    }

    override suspend fun resolveSessionProposal(sessionProposal: SessionProposal): WalletConnectSessionProposal = withContext(Dispatchers.Default) {
        val chainsByCaip2 = caip2Resolver.chainsByCaip2()

        WalletConnectSessionProposal(
            resolvedChains = SessionChains(
                required = chainsByCaip2.resolveChains(sessionProposal.requiredNamespaces.caip2ChainsByNamespace()),
                optional = chainsByCaip2.resolveChains(sessionProposal.optionalNamespaces.caip2ChainsByNamespace())
            ),
            dappMetadata = SessionDappMetadata(
                dAppUrl = sessionProposal.url,
                icon = sessionProposal.icons.firstOrNull()?.toString(),
                name = sessionProposal.name
            )
        )
    }

    override suspend fun rejectSession(proposal: SessionProposal): Result<Unit> = withContext(Dispatchers.Default) {
        val response = proposal.rejected("Rejected by user")

        Web3Wallet.rejectSession(response)
    }

    override suspend fun parseSessionRequest(request: Wallet.Model.SessionRequest): Result<WalletConnectRequest> = runCatching {
        withContext(Dispatchers.Default) {
            walletConnectRequestFactory.create(request) ?: throw WalletConnectError.UnknownMethod(request.request.method)
        }
    }

    override suspend fun onSessionSettled(settledSessionResponse: Wallet.Model.SettledSessionResponse) {
        if (settledSessionResponse !is Wallet.Model.SettledSessionResponse.Result) return

        val pairingTopic = settledSessionResponse.session.pairingTopic
        val pendingSessionSettlement = pendingSessionSettlementsByPairingTopic[pairingTopic] ?: return

        val walletConnectPairingAccount = WalletConnectPairingAccount(metaId = pendingSessionSettlement.metaId, pairingTopic = pairingTopic)
        walletConnectPairingRepository.addPairingAccount(walletConnectPairingAccount)
        walletConnectSessionRepository.addSession(settledSessionResponse.session)
    }

    override suspend fun onSessionDelete(sessionDelete: Wallet.Model.SessionDelete) {
        if (sessionDelete !is Wallet.Model.SessionDelete.Success) return

        walletConnectSessionRepository.removeSession(sessionDelete.topic)
    }

    override suspend fun getSession(sessionTopic: String): Wallet.Model.Session? {
        return walletConnectSessionRepository.getSession(sessionTopic)
    }

    override suspend fun getPairingAccount(pairingTopic: String): WalletConnectPairingAccount? {
        return walletConnectPairingRepository.getPairingAccount(pairingTopic)
    }

    override fun activeSessionsFlow(metaId: Long?): Flow<List<WalletConnectSession>> {
        return combine(
            walletConnectSessionRepository.allSessionsFlow(),
            walletConnectPairingRepository.allPairingAccountsFlow()
        ) { activeSessions, pairingAccounts ->
            val metaAccounts = if (metaId == null) {
                accountRepository.getActiveMetaAccounts()
            } else {
                listOf(accountRepository.getMetaAccount(metaId))
            }

            createWalletSessions(activeSessions, metaAccounts, pairingAccounts)
        }
    }

    override fun activeSessionFlow(sessionTopic: String): Flow<WalletConnectSessionDetails?> {
        val sessionFlow = walletConnectSessionRepository.sessionFlow(sessionTopic)
        val chainsWrappedFlow = flowOf { caip2Resolver.chainsByCaip2() }

        return combine(sessionFlow, chainsWrappedFlow) { session, chainsByCaip2 ->
            if (session == null) return@combine null

            val pairingAccount = walletConnectPairingRepository.getPairingAccount(session.pairingTopic) ?: return@combine null
            val metaAccount = accountRepository.getMetaAccount(pairingAccount.metaId)

            createWalletSessionDetails(session, metaAccount, chainsByCaip2)
        }
    }

    override suspend fun disconnect(sessionTopic: String): Result<*> {
        return withContext(Dispatchers.Default) {
            Web3Wallet.disconnectSession(sessionTopic).onSuccess {
                walletConnectSessionRepository.removeSession(sessionTopic)
            }
        }
    }

    private infix fun Map<String, Namespace.Proposal>.mergeWith(other: Map<String, Namespace.Proposal>): Map<String, Namespace.Proposal> {
        val allNamespaceKeys = keys + other.keys

        return allNamespaceKeys.associateWith { namespace ->
            val thisProposal = get(namespace)
            val otherProposal = other[namespace]

            thisProposal.orEmpty() + otherProposal.orEmpty()
        }
    }

    private operator fun Namespace.Proposal.plus(other: Namespace.Proposal): Namespace.Proposal {
        val mergedChains = chains.orEmpty() + other.chains.orEmpty()
        val mergedMethods = methods + other.methods
        val mergedEvents = events + other.events
        return Namespace.Proposal(
            chains = mergedChains.distinct(),
            methods = mergedMethods.distinct(),
            events = mergedEvents.distinct()
        )
    }

    private fun Namespace.Proposal?.orEmpty(): Namespace.Proposal {
        return this ?: Namespace.Proposal(
            chains = null,
            methods = emptyList(),
            events = emptyList()
        )
    }

    private fun formatWalletConnectAccount(address: String, chainCaip2: String): String {
        return "$chainCaip2:$address"
    }

    private fun registerPendingSettlement(sessionProposal: SessionProposal, metaAccount: MetaAccount) {
        pendingSessionSettlementsByPairingTopic[sessionProposal.pairingTopic] = PendingSessionSettlement(metaAccount.id)
    }

    private class PendingSessionSettlement(val metaId: Long)

    private fun createWalletSessions(
        sessions: List<Wallet.Model.Session>,
        metaAccounts: List<MetaAccount>,
        pairingAccounts: List<WalletConnectPairingAccount>
    ): List<WalletConnectSession> {
        val metaAccountsById = metaAccounts.associateBy(MetaAccount::id)
        val pairingAccountByPairingTopic = pairingAccounts.associateBy(WalletConnectPairingAccount::pairingTopic)

        return sessions.mapNotNull { session ->
            val pairingAccount = pairingAccountByPairingTopic[session.pairingTopic] ?: return@mapNotNull null
            val metaAccount = metaAccountsById[pairingAccount.metaId] ?: return@mapNotNull null

            WalletConnectSession(
                connectedMetaAccount = metaAccount,
                dappMetadata = session.metaData?.let(::mapAppMetadataToSessionMetadata),
                sessionTopic = session.topic
            )
        }
    }

    private fun createWalletSessionDetails(
        session: Wallet.Model.Session,
        metaAccount: MetaAccount,
        chainsByCaip2: Map<String, Chain>,
    ): WalletConnectSessionDetails {
        return WalletConnectSessionDetails(
            connectedMetaAccount = metaAccount,
            dappMetadata = session.metaData?.let(::mapAppMetadataToSessionMetadata),
            sessionTopic = session.topic,
            chains = chainsByCaip2.resolveChains(session.namespaces.caip2ChainsByNamespace()).knownChains,
            status = determineSessionStatus(session)
        )
    }

    private fun Map<String, Chain>.resolveChains(namespaces: Map<String, List<Caip2ChainId>?>): SessionChains.ResolvedChains {
        val knownChainsBuilder = mutableSetOf<Chain>()
        val unknownChainsBuilder = mutableSetOf<Caip2ChainId>()

        namespaces.forEach { (namespaceName, namespaceChains) ->
            if (caip2Parser.isValidCaip2(namespaceName)) {
                resolveChain(namespaceName, knownChainsBuilder, unknownChainsBuilder)
                return@forEach
            }

            namespaceChains.orEmpty().forEach { chainCaip2 ->
                resolveChain(chainCaip2, knownChainsBuilder, unknownChainsBuilder)
            }
        }

        return SessionChains.ResolvedChains(knownChainsBuilder.toImmutable(), unknownChainsBuilder.toImmutable())
    }

    private fun Map<String, Chain>.resolveChain(
        chainCaip2: String,
        knownChainsBuilder: KnownChainsBuilder,
        unknownChainsBuilder: UnknownChainsBuilder
    ) {
        val newChain = get(chainCaip2)

        if (newChain != null) {
            knownChainsBuilder.add(newChain)
        } else {
            unknownChainsBuilder.add(chainCaip2)
        }
    }

    private fun mapAppMetadataToSessionMetadata(metadata: Core.Model.AppMetaData): SessionDappMetadata {
        return SessionDappMetadata(
            dAppUrl = metadata.url,
            icon = metadata.icons.firstOrNull(),
            name = metadata.name
        )
    }

    private fun determineSessionStatus(session: Wallet.Model.Session): SessionStatus {
        return if (session.expiry > System.currentTimeMillis()) {
            SessionStatus.EXPIRED
        } else {
            SessionStatus.ACTIVE
        }
    }

    @JvmName("caip2ChainsByNamespaceForProposal")
    private fun Map<String, Namespace.Proposal>.caip2ChainsByNamespace(): Map<String, List<Caip2ChainId>?> {
        return mapValues { it.value.chains }
    }

    @JvmName("caip2ChainsByNamespaceForSession")
    private fun Map<String, Namespace.Session>.caip2ChainsByNamespace(): Map<String, List<Caip2ChainId>?> {
        return mapValues { it.value.chains }
    }
}
