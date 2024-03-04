package io.novafoundation.nova.feature_proxy_api.data.model

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType

data class ProxyPermission(
    val proxiedAccountId: AccountIdKey,
    val proxyAccountId: AccountIdKey,
    val proxyType: ProxyType
)
