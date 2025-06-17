package io.novafoundation.nova.common.address

import io.novafoundation.nova.common.utils.HexString
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId

class AccountIdKey(val value: AccountId) {

    companion object;

    override fun equals(other: Any?): Boolean {
        return this === other || other is AccountIdKey && this.value contentEquals other.value
    }

    override fun hashCode(): Int = value.contentHashCode()

    override fun toString(): String = value.contentToString()
}

fun AccountId.intoKey() = AccountIdKey(this)

fun AccountIdKey.toHex(): HexString {
    return value.toHexString()
}

fun AccountIdKey.toHexWithPrefix(): HexString {
    return value.toHexString(withPrefix = true)
}

fun AccountIdKey.Companion.fromHex(src: HexString): Result<AccountIdKey> {
    return runCatching { src.fromHex().intoKey() }
}

fun AccountIdKey.Companion.fromHexOrNull(src: HexString): AccountIdKey? {
    return fromHex(src).getOrNull()
}

fun AccountIdKey.Companion.fromHexOrThrow(src: HexString): AccountIdKey {
    return fromHex(src).getOrThrow()
}

operator fun <T> Map<AccountIdKey, T>.get(key: AccountId) = get(AccountIdKey(key))
fun <T> Map<AccountIdKey, T>.getValue(key: AccountId) = getValue(AccountIdKey(key))

interface WithAccountId {

    val accountId: AccountIdKey
}
