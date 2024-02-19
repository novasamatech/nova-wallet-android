package io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs

import io.novafoundation.nova.common.data.mappers.mapCryptoTypeToEncryption
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedAccount
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedMetadataKnown
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix

class PolkadotJsExtensionInteractor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val runtimeVersionsRepository: RuntimeVersionsRepository,
) {

    suspend fun getInjectedAccounts(): List<InjectedAccount> {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val defaultAccount = metaAccount.defaultSubstrateAddress?.let { address ->
            InjectedAccount(
                address = address,
                genesisHash = null,
                name = metaAccount.name,
                encryption = metaAccount.substrateCryptoType?.let { mapCryptoTypeToEncryption(it) }
            )
        }

        val customAccounts = metaAccount.chainAccounts.mapNotNull { (chainId, chainAccount) ->
            val chain = chainRegistry.getChain(chainId)

            // TODO ignore ethereum accounts for now (not all dApps support addresses in ethereum formats)
            if (chain.isEthereumBased) return@mapNotNull null

            InjectedAccount(
                address = chain.addressOf(chainAccount.accountId),
                genesisHash = chain.requireGenesisHash().requireHexPrefix(),
                name = "${metaAccount.name} (${chain.name})",
                encryption = chainAccount.cryptoType?.let(::mapCryptoTypeToEncryption)
            )
        }

        return buildList {
            defaultAccount?.let(::add)
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
}
