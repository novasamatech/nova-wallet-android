package io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs

import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_account_api.domain.model.substrateFrom
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedAccount
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.InjectedMetadataKnown
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.ext.toEthereumAddress
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.RuntimeVersionsRepository
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix

class PolkadotJsExtensionInteractor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val runtimeVersionsRepository: RuntimeVersionsRepository,
) {

    suspend fun getInjectedAccounts(): List<InjectedAccount> {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        val defaultSubstrateAccount = metaAccount.defaultSubstrateAddress?.let { address ->
            InjectedAccount(
                address = address,
                genesisHash = null,
                name = metaAccount.name,
                encryption = metaAccount.substrateCryptoType?.let { MultiChainEncryption.substrateFrom(it) }
            )
        }

        val defaultEthereumAccount = metaAccount.ethereumAddress?.let { adddressBytes ->
            InjectedAccount(
                address = adddressBytes.toEthereumAddress(),
                genesisHash = null,
                name = metaAccount.name,
                encryption = MultiChainEncryption.Ethereum
            )
        }

        val customAccounts = metaAccount.chainAccounts.mapNotNull { (chainId, chainAccount) ->
            val chain = chainRegistry.getChain(chainId)
            // Ignore non-substrate chains since they don't have chainId=genesisHash
            val genesisHash = chain.genesisHash?.requireHexPrefix() ?: return@mapNotNull null

            InjectedAccount(
                address = chain.addressOf(chainAccount.accountId),
                genesisHash = genesisHash,
                name = "${metaAccount.name} (${chain.name})",
                encryption = chainAccount.multiChainEncryption(chain)
            )
        }

        return buildList {
            defaultSubstrateAccount?.let(::add)
            defaultEthereumAccount?.let(::add)
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

    private fun MetaAccount.ChainAccount.multiChainEncryption(chain: Chain): MultiChainEncryption? {
        return if (chain.isEthereumBased) {
            MultiChainEncryption.Ethereum
        } else {
            cryptoType?.let { MultiChainEncryption.substrateFrom(it) }
        }
    }
}
