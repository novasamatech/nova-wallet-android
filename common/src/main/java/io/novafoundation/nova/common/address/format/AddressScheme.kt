package io.novafoundation.nova.common.address.format

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.runtime.AccountId

enum class AddressScheme {

    /**
     * 20-byte address, Ethereum-like address encoding
     */
    EVM,

    /**
     * 32-byte address, ss58 address encoding
     */
    SUBSTRATE;

    companion object {
        fun findFromAccountId(accountId: AccountId): AddressScheme? {
            return when (accountId.size) {
                32 -> SUBSTRATE
                20 -> EVM
                else -> null
            }
        }
    }
}

fun AccountIdKey.getAddressScheme(): AddressScheme? {
    return AddressScheme.findFromAccountId(value)
}

fun AccountIdKey.getAddressSchemeOrThrow(): AddressScheme {
    return requireNotNull(getAddressScheme()) {
        "Could not detect address scheme from account id of length ${value.size}"
    }
}

val AddressScheme.defaultOrdering
    get() = when (this) {
        AddressScheme.SUBSTRATE -> 0
        AddressScheme.EVM -> 1
    }

fun AddressScheme.isSubstrate(): Boolean {
    return this == AddressScheme.SUBSTRATE
}

fun AddressScheme.isEvm(): Boolean {
    return this == AddressScheme.EVM
}
