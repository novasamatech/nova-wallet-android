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
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountId
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_impl.data.mappers.mapProxyTypeToString
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

class RealProxyRepository(
    private val remoteSource: StorageDataSource,
    private val chainRegistry: ChainRegistry
) : ProxyRepository {

    override suspend fun getProxyDelegatorsForAccounts(chainId: ChainId, metaAccountIds: List<MetaAccountId>): List<ProxiedWithProxy> {
        val delegatorToProxies = receiveAllProxies(chainId)

        val accountIdToMetaAccounts = metaAccountIds.groupBy { it.accountId.intoKey() }

        return delegatorToProxies
            .mapNotNull { (delegator, proxies) ->
                val matchedProxies = matchProxiesToAccountsAndMap(proxies, accountIdToMetaAccounts)

                if (matchedProxies.isEmpty()) return@mapNotNull null

                delegator to matchedProxies
            }.flatMap { (delegator, proxies) ->
                proxies.map { proxy -> mapToProxiedWithProxies(chainId, delegator, proxy) }
            }
    }

    override suspend fun getDelegatedProxyTypes(chainId: ChainId, accountId: AccountId): List<ProxyAccount.ProxyType> {
        val proxies = remoteSource.query(chainId) {
            runtime.metadata.module(Modules.PROXY)
                .storage("Proxies")
                .query(
                    keyArguments = arrayOf(AccountIdKey(accountId)),
                    binding = { result ->
                        bindProxyAccounts(result)
                    }
                )
        }

        return proxies.map { mapProxyTypeToString(it.value) }
    }

    private suspend fun receiveAllProxies(chainId: ChainId): Map<AccountIdKey, Map<AccountIdKey, String>> {
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

    private fun bindProxyAccounts(dynamicInstance: Any?): Map<AccountIdKey, String> {
        val root = dynamicInstance.castToList()
        val proxies = root[0].castToList()

        return proxies.map {
            val proxy = it.castToStruct()
            val proxyAccountId: ByteArray = proxy.getTyped("delegate")
            val proxyType = proxy.get<Any?>("proxyType").castToDictEnum()
            proxyAccountId.intoKey() to proxyType.name
        }.toMap()
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
        proxies: Map<AccountIdKey, String>,
        accountIdToMetaAccounts: Map<AccountIdKey, List<MetaAccountId>>
    ): List<ProxiedWithProxy.Proxy> {
        return proxies.flatMap { (proxyAccountId, proxyType) ->
            val matchedAccounts = accountIdToMetaAccounts[proxyAccountId] ?: return@flatMap emptyList()

            matchedAccounts.map {
                ProxiedWithProxy.Proxy(
                    accountId = proxyAccountId.value,
                    metaId = it.metaId,
                    proxyType = proxyType
                )
            }
        }
    }
}
