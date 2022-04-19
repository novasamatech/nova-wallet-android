package io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.mainEthereumAddress
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.EthereumAddress
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.findChain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MetamaskInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) {

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun getAddresses(ethereumChainId: String): Set<EthereumAddress> = withContext(Dispatchers.Default) {
        val selectedAccount = accountRepository.getSelectedMetaAccount()
        val maybeChain = tryFindChainFromEthereumChainId(ethereumChainId)

        val chainsById = chainRegistry.chainsById.first()

        val selectedAddress = maybeChain?.let { selectedAccount.addressIn(it) }

        val mainAddress = selectedAccount.mainEthereumAddress()

        val chainAccountAddresses = selectedAccount.chainAccounts
            .mapNotNull { (chainId, chainAccount) ->
                val chain = chainsById[chainId]

                chain?.addressOf(chainAccount.accountId)?.takeIf {
                    chain.isEthereumBased && chainAccount.cryptoType == CryptoType.ECDSA
                }
            }

        buildSet {
            selectedAddress?.let { add(it) }
            mainAddress?.let { add(it) }
            addAll(chainAccountAddresses)
        }
    }

    private suspend fun tryFindChainFromEthereumChainId(ethereumChainId: String): Chain? {
        val addressPrefix = ethereumChainId.toIntOrNull(radix = 16) ?: return null

        return chainRegistry.findChain { it.isEthereumBased && it.addressPrefix == addressPrefix }
    }
}
