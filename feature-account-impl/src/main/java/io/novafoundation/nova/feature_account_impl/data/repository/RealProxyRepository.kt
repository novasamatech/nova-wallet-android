package io.novafoundation.nova.feature_account_impl.data.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.binding.getTyped
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.feature_account_api.data.model.ProxiedWithProxy
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountId
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_impl.data.mappers.mapProxyTypeToString
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import java.math.BigInteger

private class OnChainProxyModel(
    val accountId: AccountIdKey,
    val proxyType: String,
    val delay: BigInteger
)

class RealProxyRepository(
    private val remoteSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : ProxyRepository {

    override suspend fun getAllProxiesForMetaAccounts(chainId: ChainId, metaAccountIds: List<MetaAccountId>): List<ProxiedWithProxy> {
        val delegatorToProxies = receiveAllProxies(chainId)

        val accountIdToMetaAccounts = metaAccountIds.groupBy { it.accountId.intoKey() }

        return delegatorToProxies
            .mapNotNull { (delegator, proxies) ->
                val notDelayedProxies = proxies.filter { it.delay == BigInteger.ZERO }
                val matchedProxies = matchProxiesToAccountsAndMap(notDelayedProxies, accountIdToMetaAccounts)

                if (matchedProxies.isEmpty()) return@mapNotNull null

                delegator to matchedProxies
            }.flatMap { (delegator, proxies) ->
                proxies.map { proxy -> mapToProxiedWithProxies(chainId, delegator, proxy) }
            }
    }

    override suspend fun getDelegatedProxyTypes(chainId: ChainId, proxiedAccountId: AccountId, proxyAccountId: AccountId): List<ProxyAccount.ProxyType> {
        val proxies = remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .query(
                    keyArguments = arrayOf(proxiedAccountId),
                    binding = { result ->
                        bindProxyAccounts(result)
                    }
                )
        }

        return proxies.filter { it.accountId == proxyAccountId.intoKey() }
            .map { mapProxyTypeToString(it.proxyType) }
    }

    private suspend fun receiveAllProxies(chainId: ChainId): Map<AccountIdKey, List<OnChainProxyModel>> {
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

    private fun bindProxyAccounts(dynamicInstance: Any?): List<OnChainProxyModel> {
        if (dynamicInstance == null) return emptyList()

        val root = dynamicInstance.castToList()
        val proxies = root[0].castToList()

        return proxies.map {
            val proxy = it.castToStruct()
            val proxyAccountId: ByteArray = proxy.getTyped("delegate")
            val proxyType = proxy.get<Any?>("proxyType").castToDictEnum()
            val delay = proxy.getTyped<BigInteger>("delay")
            OnChainProxyModel(
                proxyAccountId.intoKey(),
                proxyType.name,
                delay
            )
        }
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
        accountIdToMetaAccounts: Map<AccountIdKey, List<MetaAccountId>>
    ): List<ProxiedWithProxy.Proxy> {
        return proxies.flatMap { onChainProxy ->
            val matchedAccounts = accountIdToMetaAccounts[onChainProxy.accountId] ?: return@flatMap emptyList()

            matchedAccounts.map {
                ProxiedWithProxy.Proxy(
                    accountId = onChainProxy.accountId.value,
                    metaId = it.metaId,
                    proxyType = onChainProxy.proxyType
                )
            }
        }
    }
}
