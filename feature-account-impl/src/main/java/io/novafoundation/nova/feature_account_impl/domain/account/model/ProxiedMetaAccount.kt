package io.novafoundation.nova.feature_account_impl.domain.account.model

import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ProxiedMetaAccount(
    id: Long,
    globallyUniqueId: String,
    substratePublicKey: ByteArray?,
    substrateCryptoType: CryptoType?,
    substrateAccountId: ByteArray?,
    ethereumAddress: ByteArray?,
    ethereumPublicKey: ByteArray?,
    isSelected: Boolean,
    name: String,
    type: LightMetaAccount.Type,
    status: LightMetaAccount.Status,
    override val proxy: ProxyAccount,
    chainAccounts: Map<ChainId, MetaAccount.ChainAccount>
) : DefaultMetaAccount(
    id = id,
    globallyUniqueId = globallyUniqueId,
    substratePublicKey = substratePublicKey,
    substrateCryptoType = substrateCryptoType,
    substrateAccountId = substrateAccountId,
    ethereumAddress = ethereumAddress,
    ethereumPublicKey = ethereumPublicKey,
    isSelected = isSelected,
    name = name,
    type = type,
    status = status,
    chainAccounts = chainAccounts,
    proxy = proxy
) {

    private val proxyChainToRemoteProxies = mapOf(
        Chain.Geneses.KUSAMA to listOf(Chain.Geneses.KUSAMA_ASSET_HUB)
    )

    override val chainAccounts = buildProxiableChainAccounts(id, chainAccounts)

    fun isRemoteProxyChain(chainId: ChainId): Boolean {
        return chainId != proxy.chainId
    }

    fun getMainProxyChainId(): ChainId {
        return proxy.chainId
    }

    private fun buildProxiableChainAccounts(
        metaId: Long,
        chainAccounts: Map<ChainId, MetaAccount.ChainAccount>
    ): Map<ChainId, MetaAccount.ChainAccount> {
        val onlyChainAccount = chainAccounts.entries.first()
        val proxyChainId = onlyChainAccount.key
        val proxyAccount = onlyChainAccount.value

        return buildMap {
            put(proxyChainId, proxyAccount)

            val remoteProxies = proxyChainToRemoteProxies[proxyChainId].orEmpty()
            remoteProxies.forEach {
                put(it, MetaAccount.ChainAccount(metaId, it, null, proxyAccount.accountId, null))
            }
        }
    }

    override suspend fun supportsAddingChainAccount(chain: Chain): Boolean {
        // User cannot manually add accounts to proxy meta account
        return false
    }
}
