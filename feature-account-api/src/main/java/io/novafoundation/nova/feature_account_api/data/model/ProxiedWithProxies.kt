package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.runtime.AccountId

class ProxiedWithProxies(
    val accountId: AccountId,
    val chainId: ChainId,
    val proxies: List<Proxy>
) {
    class Proxy(
        val accountId: AccountId,
        val metaId: Long,
        val proxyType: String
    )
}
