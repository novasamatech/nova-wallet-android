package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal data class BittensorValidatorLink(val hotkey: AccountIdKey, val netuid: Int)

/** Presence of a parent OR child link, not a claim about that linked key's permit or activity. */
internal suspend fun StorageQueryContext.loadBittensorValidatorLinks(pairs: List<BittensorValidatorLink>): Set<BittensorValidatorLink> = coroutineScope {
    if (pairs.isEmpty()) return@coroutineScope emptySet()
    val keys = pairs.distinct().map { listOf(it.hotkey.value, it.netuid.toBigInteger()) }
    suspend fun read(name: String): Set<BittensorValidatorLink> = runtime.metadata.module("SubtensorModule").storage(name)
        .entries(
            keysArguments = keys,
            keyExtractor = { key -> BittensorValidatorLink(AccountIdKey(key.component1()), key.component2<BigInteger>().toInt()) },
            binding = { value, _ -> value != null && bindList(value) { it }.isNotEmpty() }
        ).filterValues { it }.keys

    val parents = async { read("ParentKeys") }
    val children = async { read("ChildKeys") }
    parents.await() + children.await()
}
