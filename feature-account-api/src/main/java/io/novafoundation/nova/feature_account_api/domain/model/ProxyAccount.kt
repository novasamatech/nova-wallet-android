package io.novafoundation.nova.feature_account_api.domain.model

import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class ProxyAccount(
    val proxyMetaId: Long,
    val chainId: ChainId,
    val proxyType: ProxyType,
)
