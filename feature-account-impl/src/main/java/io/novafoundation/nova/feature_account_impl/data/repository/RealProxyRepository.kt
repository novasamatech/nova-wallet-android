package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount.ProxyAccount
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

class RealProxyRepository(
    private val remoteSource: StorageDataSource
) : ProxyRepository {

    override suspend fun getProxyDelegatorsForAccounts(chainId: ChainId, chainAccounts: List<MetaAccount.ChainAccount>) {
        val allProxiesOnChain = remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .entries { result, storageKeyComponent ->
                    storageKeyComponent.values
                    bindProxyAccounts(result)
                }
        }
    }

    private fun bindProxyAccounts(dynamicInstance: Any?): List<Pair<AccountId, ProxyAccount.ProxyType>> {
        val delegates = dynamicInstance.castToList()
        val proxies = delegates[0].castToStruct()
        val proxyAccountId: ByteArray = proxies.getTyped("delegate")
        val proxyType: String = proxies.getTyped("proxyType")
        return listOf()
    }
}
