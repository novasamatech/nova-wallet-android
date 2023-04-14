package io.novafoundation.nova.feature_dapp_impl.domain.browser.metamask

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.domain.model.mainEthereumAddress
import io.novafoundation.nova.feature_dapp_impl.web3.metamask.model.EthereumAddress
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.findEvmChainFromHexId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MetamaskInteractor(
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry
) {

    suspend fun getAddresses(ethereumChainId: String): List<EthereumAddress> = withContext(Dispatchers.Default) {
        val selectedAccount = accountRepository.getSelectedMetaAccount()
        val maybeChain = chainRegistry.findEvmChainFromHexId(ethereumChainId)

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

        buildList {
            selectedAddress?.let { add(it) }
            mainAddress?.let { add(it) }
            addAll(chainAccountAddresses)
        }.distinct()
    }
}
