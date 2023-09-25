package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.common.address.AccountIdKey

@Deprecated("Use AccountIdKeyMap instead")
typealias AccountIdMap<V> = Map<String, V>

@Deprecated("Use AccountIdKeyMap instead")
typealias AccountAddressMap<V> = Map<String, V>

typealias AccountIdKeyMap<V> = Map<AccountIdKey, V>
