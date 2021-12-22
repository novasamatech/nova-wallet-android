package io.novafoundation.nova.feature_dapp_impl.domain.browser

import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.util.UrlNormalizer
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedAccount
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DappBrowserInteractor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository
) {

    suspend fun getDAppInfo(dAppUrl: String): DAppInfo = withContext(Dispatchers.Default) {
        DAppInfo(
            baseUrl = UrlNormalizer.normalizeUrl(dAppUrl),
            metadata = null // TODO whitelist task
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun getInjectedAccounts(): List<InjectedAccount> {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val defaultAccount = InjectedAccount(
            address = metaAccount.defaultSubstrateAddress,
            genesisHash = null,
            name = metaAccount.name,
            encryption = mapCryptoTypeToEncryption(metaAccount.substrateCryptoType)
        )

        val customAccounts = metaAccount.chainAccounts.mapNotNull { (chainId, chainAccount) ->
            val chain = chainRegistry.getChain(chainId)

            // TODO ignore ethereum accounts for now (not all dApps support addresses in ethereum formats)
            if (chain.isEthereumBased) return@mapNotNull null

            InjectedAccount(
                address = chain.addressOf(chainAccount.accountId),
                genesisHash = chain.genesisHash,
                name = "${metaAccount.name} (${chain.name})",
                encryption = mapCryptoTypeToEncryption(chainAccount.cryptoType)
            )
        }

        return buildList {
            add(defaultAccount)
            addAll(customAccounts)
        }
    }
}
