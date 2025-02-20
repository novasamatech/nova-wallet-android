package io.novafoundation.nova.common.data.network.runtime.binding

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.intoKey
import io.novasama.substrate_sdk_android.runtime.AccountId

@HelperBinding
fun bindAccountId(dynamicInstance: Any?): AccountId = dynamicInstance.cast()

fun bindAccountIdKey(dynamicInstance: Any?): AccountIdKey = bindAccountId(dynamicInstance).intoKey()

fun bindNullableAccountId(dynamicInstance: Any?): AccountId? = dynamicInstance.nullableCast()
