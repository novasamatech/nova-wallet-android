package io.novafoundation.nova.feature_proxy_api.data.model

import java.math.BigInteger

class OnChainProxiedModel(
    val proxies: List<OnChainProxyModel>,
    val deposit: BigInteger
)
