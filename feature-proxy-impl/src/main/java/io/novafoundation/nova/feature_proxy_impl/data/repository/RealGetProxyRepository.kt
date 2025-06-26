package io.novafoundation.nova.feature_proxy_impl.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.proxy
import io.novafoundation.nova.feature_proxy_api.data.model.OnChainProxiedModel
import io.novafoundation.nova.feature_proxy_api.data.model.OnChainProxyModel
import io.novafoundation.nova.feature_proxy_api.data.model.ProxiesMap
import io.novafoundation.nova.feature_proxy_api.data.model.ProxyPermission
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.model.fromString
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

class RealGetProxyRepository(
    private val remoteSource: StorageDataSource,
    private val localSource: StorageDataSource,
    private val chainRegistry: ChainRegistry,
) : GetProxyRepository {

    override suspend fun getAllProxies(chainId: ChainId): ProxiesMap {
        return receiveAllProxiesInChain(chainId)
    }

    override suspend fun getDelegatedProxyTypesRemote(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyType> {
        return getDelegatedProxyTypes(remoteSource, chainId, proxiedAccountId, proxyAccountId)
    }

    // TODO: use it for staking after merge "add staking proxy" branch
    override suspend fun getDelegatedProxyTypesLocal(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyType> {
        return getDelegatedProxyTypes(localSource, chainId, proxiedAccountId, proxyAccountId)
    }

    override suspend fun getProxiesQuantity(chainId: ChainId, proxiedAccountId: AccountId): Int {
        val proxied = getAllProxiesFor(localSource, chainId, proxiedAccountId)

        return proxied.proxies.size
    }

    override suspend fun getProxyDeposit(chainId: ChainId, proxiedAccountId: AccountId): BigInteger {
        val proxied = getAllProxiesFor(localSource, chainId, proxiedAccountId)

        return proxied.deposit
    }

    override suspend fun maxProxiesQuantity(chain: Chain): Int {
        val runtime = chainRegistry.getRuntime(chain.id)
        val constantQuery = runtime.metadata.proxy()
        return constantQuery.numberConstant("MaxProxies", runtime).toInt()
    }

    override fun proxiesByTypeFlow(chain: Chain, accountId: AccountId, proxyType: ProxyType): Flow<List<ProxyPermission>> {
        return localSource.subscribe(chain.id) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .observe(
                    accountId,
                    binding = { bindProxyAccounts(it) }
                )
        }.map { proxied ->
            proxied.proxies
                .filter { it.proxyType.name == proxyType.name }
                .map { ProxyPermission(accountId.intoKey(), it.proxy, it.proxyType) }
        }
    }

    override fun proxiesQuantityByTypeFlow(chain: Chain, accountId: AccountId, proxyType: ProxyType): Flow<Int> {
        return proxiesByTypeFlow(chain, accountId, proxyType)
            .map { it.size }
    }

    private suspend fun getDelegatedProxyTypes(
        storageDataSource: StorageDataSource,
        chainId: ChainId,
        proxiedAccountId: AccountId,
        proxyAccountId: AccountId
    ): List<ProxyType> {
        val proxied = getAllProxiesFor(storageDataSource, chainId, proxiedAccountId)

        return proxied.proxies
            .filter { it.proxy == proxyAccountId.intoKey() }
            .map { it.proxyType }
    }

    private suspend fun getAllProxiesFor(storageDataSource: StorageDataSource, chainId: ChainId, accountId: AccountId): OnChainProxiedModel {
        return storageDataSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .query(
                    keyArguments = arrayOf(accountId),
                    binding = { result -> bindProxyAccounts(result) }
                )
        }
    }

    private suspend fun receiveAllProxiesInChain(chainId: ChainId): Map<AccountIdKey, OnChainProxiedModel> {
        return remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .entries(
                    keyExtractor = { (accountId: AccountId) -> AccountIdKey(accountId) },
                    binding = { result, _ -> bindProxyAccounts(result) },
                    recover = { _, _ ->
                        // Do nothing if entry binding throws an exception
                    }
                )
        }
    }

    private fun bindProxyAccounts(dynamicInstance: Any?): OnChainProxiedModel {
        if (dynamicInstance == null) return OnChainProxiedModel(emptyList(), BigInteger.ZERO)

        val root = dynamicInstance.castToList()
        val proxies = root[0].castToList()

        return OnChainProxiedModel(
            proxies = proxies.map {
                val proxy = it.castToStruct()
                val proxyAccountId: ByteArray = proxy.getTyped("delegate")
                val proxyType = proxy.get<Any?>("proxyType").castToDictEnum()
                val delay = proxy.getTyped<BigInteger>("delay")
                OnChainProxyModel(
                    proxy = proxyAccountId.intoKey(),
                    proxyType = ProxyType.fromString(proxyType.name),
                    delay = delay
                )
            },
            deposit = root[1].cast()
        )
    }
}
