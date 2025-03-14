package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindSet

typealias SessionValidators = Set<AccountIdKey>

fun bindSessionValidators(dynamicInstance: Any?) = bindSet(dynamicInstance, ::bindAccountIdKey)
