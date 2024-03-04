package io.novafoundation.nova.runtime.network.updaters

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.core.model.StorageChange
import io.novafoundation.nova.core.model.StorageEntry
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

suspend fun StorageCache.insert(
    storageChange: StorageChange,
    chainId: String,
) {
    val storageEntry = StorageEntry(
        storageKey = storageChange.key,
        content = storageChange.value,
    )

    insert(storageEntry, chainId)
}

abstract class SingleStorageKeyUpdater<V>(
    override val scope: UpdateScope<V>,
    private val chainIdHolder: ChainIdHolder,
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache
) : Updater<V> {

    /**
     * @return a storage key to update. null in case updater does not want to update anything
     */
    abstract suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: V): String?

    protected open fun fallbackValue(runtime: RuntimeSnapshot): String? = null

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: V,
    ): Flow<Updater.SideEffect> {
        val chainId = chainIdHolder.chainId()
        val runtime = chainRegistry.getRuntime(chainId)

        val storageKey = runCatching { storageKey(runtime, scopeValue) }.getOrNull() ?: return emptyFlow()

        return storageSubscriptionBuilder.subscribe(storageKey)
            .map {
                if (it.value == null) {
                    it.copy(value = fallbackValue(runtime))
                } else {
                    it
                }
            }
            .onEach { storageCache.insert(it, chainId) }
            .noSideAffects()
    }
}
