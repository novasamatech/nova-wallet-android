package io.novafoundation.nova.feature_account_impl.data.proxy

import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chainsById

class RealProxySyncService(
    private val chainRegistry: ChainRegistry,
    private val proxyRepository: ProxyRepository,
    private val accounRepository: AccountRepository
) : ProxySyncService {

    override suspend fun startSyncing() {
        if (accounRepository.metaAccountFlow())
        val metaAccounts = getMetaAccount()

        val chainIdToChainAcconts = metaAccounts.flatMap { it.chainAccounts.values }
            .groupBy { it.chainId }

        val supportedProxyChains = getSupportedProxyChains()
        supportedProxyChains.get("")!!.

        for ((chainId, chainAccounts) in chainIdToChainAcconts) {
            proxyRepository.getProxyDelegatorsForAccounts(chainId, chainAccounts)
        }
    }

    override suspend fun syncForMetaAccount(metaAccount: MetaAccount) {
        TODO("provide updater to sync proxy delegators for new added accounts")
    }

    private suspend fun getMetaAccount(): List<MetaAccount> {
        return accounRepository.allMetaAccounts()
            .filter { it.type != LightMetaAccount.Type.WATCH_ONLY || true/*it.type != LightMetaAccount.Type.PROXY*/ }
    }

    private suspend fun getSupportedProxyChains(): Map<ChainId, Chain> {
        return chainRegistry.chainsById()
            .filterValues { it.supportProxy }
    }
}
