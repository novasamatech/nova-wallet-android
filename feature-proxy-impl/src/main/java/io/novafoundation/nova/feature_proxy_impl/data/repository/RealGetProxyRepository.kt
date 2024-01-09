package io.novafoundation.nova.feature_proxy_impl.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_proxy_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.model.fromString
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

private class OnChainProxiedModel(
    val proxies: List<OnChainProxyModel>,
    val deposit: BigInteger
)

private class OnChainProxyModel(
    val accountId: AccountIdKey,
    val proxyType: String,
    val delay: BigInteger
)

class RealGetProxyRepository(
    private val remoteSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : GetProxyRepository {

    override suspend fun getAllProxiesForAccounts(chainId: ChainId, accountIds: Set<AccountIdKey>): List<ProxiedWithProxy> {
        val delegatorToProxies = receiveAllProxies(chainId)

        return delegatorToProxies
            .mapNotNull { (delegator, proxied) ->
                val notDelayedProxies = proxied.proxies.filter { it.delay == BigInteger.ZERO }
                val matchedProxies = matchProxiesToAccountsAndMap(notDelayedProxies, accountIds)

                if (matchedProxies.isEmpty()) return@mapNotNull null

                delegator to matchedProxies
            }.flatMap { (delegator, proxies) ->
                proxies.map { proxy -> mapToProxiedWithProxies(chainId, delegator, proxy) }
            }
    }

    override suspend fun getDelegatedProxyTypes(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyType> {
        val proxied = getAllProxiesFor(chainId, proxiedAccountId)

        return proxied.proxies.filter { it.accountId == proxyAccountId.intoKey() }
            .map { ProxyType.fromString(it.proxyType) }
    }

    override suspend fun getProxiesQuantity(chainId: ChainId, proxiedAccountId: AccountId): Int {
        val proxied = getAllProxiesFor(chainId, proxiedAccountId)

        return proxied.proxies.size
    }

    override suspend fun getProxyDeposit(chainId: ChainId, proxiedAccountId: AccountId): BigInteger {
        val proxied = getAllProxiesFor(chainId, proxiedAccountId)

        return proxied.deposit
    }

    private suspend fun getAllProxiesFor(chainId: ChainId, accountId: AccountId): OnChainProxiedModel {
        return remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .query(
                    keyArguments = arrayOf(accountId),
                    binding = { result ->
                        bindProxyAccounts(result)
                    }
                )
        }
    }

    private suspend fun receiveAllProxies(chainId: ChainId): Map<AccountIdKey, OnChainProxiedModel> {
        return remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .entries(
                    keyExtractor = { (accountId: AccountId) -> AccountIdKey(accountId) },
                    binding = { result, _ ->
                        bindProxyAccounts(result)
                    },
                    recover = { _, _ ->
                        // Do nothing if entry binding throws an exception
                    }
                )
        }
    }

    private fun bindProxyAccounts(dynamicInstance: Any?): OnChainProxiedModel {
        val root = dynamicInstance.castToList()
        val proxies = root[0].castToList()

        return OnChainProxiedModel(
            proxies = proxies.map {
                val proxy = it.castToStruct()
                val proxyAccountId: ByteArray = proxy.getTyped("delegate")
                val proxyType = proxy.get<Any?>("proxyType").castToDictEnum()
                val delay = proxy.getTyped<BigInteger>("delay")
                OnChainProxyModel(
                    proxyAccountId.intoKey(),
                    proxyType.name,
                    delay
                )
            },
            deposit = root[1].cast()
        )
    }

    private fun mapToProxiedWithProxies(
        chainId: ChainId,
        delegator: AccountIdKey,
        proxy: ProxiedWithProxy.Proxy
    ): ProxiedWithProxy {
        return ProxiedWithProxy(
            proxied = ProxiedWithProxy.Proxied(
                accountId = delegator.value,
                chainId = chainId
            ),
            proxy = proxy
        )
    }

    private fun matchProxiesToAccountsAndMap(
        proxies: List<OnChainProxyModel>,
        accountIdToMetaAccounts: Set<AccountIdKey>
    ): List<ProxiedWithProxy.Proxy> {
        return proxies.filter {
            accountIdToMetaAccounts.contains(it.accountId)
        }.map { onChainProxy ->
            ProxiedWithProxy.Proxy(
                accountId = onChainProxy.accountId.value,
                proxyType = onChainProxy.proxyType
            )
        }
    }
}
