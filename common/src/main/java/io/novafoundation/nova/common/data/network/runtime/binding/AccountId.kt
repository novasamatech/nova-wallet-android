package io.novafoundation.nova.common.data.network.runtime.binding

import io.novasama.substrate_sdk_android.runtime.AccountId

@HelperBinding
fun bindAccountId(dynamicInstance: Any?): AccountId = dynamicInstance.cast()
fun bindNullableAccountId(dynamicInstance: Any?): AccountId? = dynamicInstance.nullableCast()
