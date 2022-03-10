package io.novafoundation.nova.runtime.storage.source.multi

import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.module.StorageEntry
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKeys

class MultiQueryBuilderImpl(
    private val runtime: RuntimeSnapshot
) : MultiQueryBuilder {

    private val keys: MutableMap<StorageEntry, MutableList<String>> = mutableMapOf()

    override fun StorageEntry.queryKey(vararg args: Any?) {
        keysForEntry(this).add(storageKey(runtime, *args))
    }

    override fun StorageEntry.queryKeys(keysArgs: List<List<Any?>>) {
        keysForEntry(this).addAll(storageKeys(runtime, keysArgs))
    }

    fun build(): Map<StorageEntry, List<String>> {
        return keys
    }

    private fun keysForEntry(entry: StorageEntry) = keys.getOrPut(entry, ::mutableListOf)
}
