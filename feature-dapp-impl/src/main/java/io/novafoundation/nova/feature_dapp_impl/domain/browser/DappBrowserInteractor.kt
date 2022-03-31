package io.novafoundation.nova.feature_dapp_impl.domain.browser

import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.data.repository.PhishingSitesRepository
import io.novafoundation.nova.feature_dapp_impl.util.Urls
import io.novafoundation.nova.feature_dapp_impl.util.isSecure
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedAccount
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedMetadataKnown
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class DappBrowserInteractor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val phishingSitesRepository: PhishingSitesRepository,
    private val runtimeVersionsRepository: RuntimeVersionsRepository,
) {

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
                genesisHash = chain.genesisHash.requireHexPrefix(),
                name = "${metaAccount.name} (${chain.name})",
                encryption = mapCryptoTypeToEncryption(chainAccount.cryptoType)
            )
        }

        return buildList {
            add(defaultAccount)
            addAll(customAccounts)
        }
    }

    suspend fun getKnownInjectedMetadatas(): List<InjectedMetadataKnown> {
        return runtimeVersionsRepository.getAllRuntimeVersions().map {
            InjectedMetadataKnown(
                genesisHash = it.chainId.requireHexPrefix(),
                specVersion = it.specVersion
            )
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    suspend fun browserPageFor(fullUrl: String, synchronizedWithBrowser: Boolean): BrowserPage = withContext(Dispatchers.Default) {
        runCatching {
            val security = when {
                phishingSitesRepository.isPhishing(fullUrl) -> BrowserPage.Security.DANGEROUS
                URL(fullUrl).isSecure -> BrowserPage.Security.SECURE
                else -> BrowserPage.Security.UNKNOWN
            }
            BrowserPage(display = Urls.hostOf(fullUrl), security = security, synchronizedWithBrowser = synchronizedWithBrowser)
        }.getOrElse {
            BrowserPage(display = fullUrl, security = BrowserPage.Security.UNKNOWN, synchronizedWithBrowser = synchronizedWithBrowser)
        }
    }
}
