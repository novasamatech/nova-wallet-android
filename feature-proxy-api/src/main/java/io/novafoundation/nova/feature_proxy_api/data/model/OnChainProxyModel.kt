package io.novafoundation.nova.feature_proxy_api.data.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import java.math.BigInteger

class OnChainProxyModel(
    val proxy: AccountIdKey,
    val proxyType: ProxyType,
    val delay: BigInteger
)
