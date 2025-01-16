package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.bindings

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindList

typealias SessionValidators = List<AccountIdKey>

fun bindSessionValidators(dynamicInstance: Any?) = bindList(dynamicInstance, ::bindAccountIdKey)
