package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

fun StorageEntry.accountMapStorageKeys(runtime: RuntimeSnapshot, accountIdsHex: List<String>): List<String> {
    return accountIdsHex.map { storageKey(runtime, it.fromHex()) }
}

fun String.accountIdFromMapKey() = fromHex().takeLast(32).toByteArray().toHexString()
