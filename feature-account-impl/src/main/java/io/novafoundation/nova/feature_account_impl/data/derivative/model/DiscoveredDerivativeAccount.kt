package io.novafoundation.nova.feature_account_impl.data.derivative.model

import io.novafoundation.nova.common.address.AccountIdKey

class DiscoveredDerivativeAccount(
    val parent: AccountIdKey,
    val derivative: AccountIdKey,
    val index: Int
)
