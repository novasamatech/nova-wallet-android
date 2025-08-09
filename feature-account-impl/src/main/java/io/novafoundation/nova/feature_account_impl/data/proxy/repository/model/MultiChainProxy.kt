package io.novafoundation.nova.feature_account_impl.data.proxy.repository.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class MultiChainProxy(
    val chainId: ChainId,
    val proxied: AccountIdKey,
    val proxy: AccountIdKey,
    val proxyType: ProxyType,
)
