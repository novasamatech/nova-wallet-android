package io.novafoundation.nova.runtime.util

import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.RuntimeType
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.MULTI_ADDRESS_ID
import io.novasama.substrate_sdk_android.runtime.definitions.types.primitives.FixedByteArray
import io.novasama.substrate_sdk_android.runtime.definitions.types.skipAliases

fun RuntimeType<*, *>.constructAccountLookupInstance(accountId: AccountId): Any {
    return when (skipAliases()) {
        is DictEnum -> { // MultiAddress
            DictEnum.Entry(MULTI_ADDRESS_ID, accountId)
        }
        is FixedByteArray -> { // GenericAccountId or similar
            accountId
        }
        else -> throw UnsupportedOperationException("Unknown address type: ${this.name}")
    }
}
