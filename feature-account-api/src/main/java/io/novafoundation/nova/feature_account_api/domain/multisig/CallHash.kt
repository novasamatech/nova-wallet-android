package io.novafoundation.nova.feature_account_api.domain.multisig

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey

// TODO multisig: we are using `AccountIdKey` as it logically represents the `DataByteArray`
// We need to create DataByteArray class that AccountIdKey will typealias to
typealias CallHash = AccountIdKey

fun bindCallHash(decoded: Any?) = bindAccountIdKey(decoded)
