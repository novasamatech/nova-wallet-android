package io.novafoundation.nova.feature_account_impl.data.proxy.network.api.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.common.utils.HexString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class FindProxiesResponse(
    val proxieds: SubQueryNodes<ProxiedRemote>
)

class ProxiedRemote(
    val accountId: HexString,
    val type: String,
    val proxyAccountId: HexString,
    val chainId: ChainId,
)
