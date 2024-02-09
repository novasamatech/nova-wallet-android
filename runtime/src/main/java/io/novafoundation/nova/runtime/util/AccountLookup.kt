package io.novafoundation.nova.runtime.util

import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.RuntimeType
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.composite.DictEnum
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.MULTI_ADDRESS_ID
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.primitives.FixedByteArray
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.skipAliases

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
