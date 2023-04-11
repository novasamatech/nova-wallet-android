package io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.session

import com.walletconnect.web3.wallet.client.Wallet
import com.walletconnect.web3.wallet.client.Wallet.Model.SessionProposal
import com.walletconnect.web3.wallet.client.Web3Wallet
import io.novafoundation.nova.common.utils.mapValuesNotNull
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.approveSession
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.approved
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.rejectSession
import io.novafoundation.nova.feature_dapp_impl.walletConnect.domain.sdk.rejected
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.web3names.data.caip19.Caip19MatcherFactory
import io.novafoundation.nova.web3names.data.caip19.identifiers.Caip2Namespace
import kotlinx.coroutines.flow.first

interface WalletConnectSessionInteractor {

    suspend fun approveSession(sessionProposal: SessionProposal): Result<Unit>

    suspend fun rejectSession(proposal: SessionProposal): Result<Unit>
}

class RealWalletConnectSessionInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val caip19MatcherFactory: Caip19MatcherFactory,
) : WalletConnectSessionInteractor {

    override suspend fun approveSession(sessionProposal: SessionProposal): Result<Unit> {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val requestedNameSpaces = sessionProposal.requiredNamespaces + sessionProposal.optionalNamespaces

        val localChains = chainRegistry.currentChains.first()

        val namespaceSessions = requestedNameSpaces.mapValuesNotNull { (namespaceRaw, namespaceProposal) ->
            // TODO handle https://docs.walletconnect.com/2.0/specs/clients/sign/namespaces#13-chains-might-be-omitted-if-the-caip-2-is-defined-in-the-index
            val namespace = Caip2Namespace.find(namespaceRaw) ?: return@mapValuesNotNull null
            val requestedChains = namespaceProposal.chains ?: return@mapValuesNotNull null

            val chainByCaip2 = localChains.associateBy { caip19MatcherFactory.caip2Of(it, preferredNamespace = namespace) }

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
    }

    override suspend fun rejectSession(proposal: SessionProposal): Result<Unit> {
        val response = proposal.rejected("Rejected by user")

        return Web3Wallet.rejectSession(response)
    }


    private fun formatWalletConnectAccount(address: String, chainCaip2: String): String {
        return "$chainCaip2:$address"
    }
}
