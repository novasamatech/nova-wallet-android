package io.novafoundation.nova.feature_proxy_api.data.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ProxiedWithProxy(
    val proxied: Proxied,
    val proxy: Proxy
) {
    class Proxied(
        val accountId: AccountId,
        val chainId: ChainId
    )

    class Proxy(
        val accountId: AccountId,
        val proxyType: String
    )
}
