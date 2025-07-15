package io.novafoundation.nova.feature_account_impl.data.proxy.repository

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.fromHexOrNull
import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.FindProxiesApi
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.request.FindProxiesRequest
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.response.FindProxiesResponse
import io.novafoundation.nova.feature_account_impl.data.proxy.repository.model.MultiChainProxy
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.model.fromString
import javax.inject.Inject

interface MultiChainProxyRepository {

    suspend fun getProxies(accountIds: Collection<AccountIdKey>): List<MultiChainProxy>
}

@FeatureScope
class RealMultiChainProxyRepository @Inject constructor(
    private val proxiesApi: FindProxiesApi,
) : MultiChainProxyRepository {

    override suspend fun getProxies(accountIds: Collection<AccountIdKey>): List<MultiChainProxy> {
        val request = FindProxiesRequest(accountIds)
        return proxiesApi.findProxies(request).toDomain()
    }

    private fun SubQueryResponse<FindProxiesResponse>.toDomain(): List<MultiChainProxy> {
        return data.proxieds.nodes.mapNotNull { proxiedNode ->
            MultiChainProxy(
                chainId = proxiedNode.chainId.removeHexPrefix(),
                proxyType = ProxyType.fromString(proxiedNode.type),
                proxied = AccountIdKey.fromHexOrNull(proxiedNode.accountId) ?: return@mapNotNull null,
                proxy = AccountIdKey.fromHexOrNull(proxiedNode.proxyAccountId) ?: return@mapNotNull null
            )
        }
    }
}
