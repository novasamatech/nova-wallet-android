package io.novafoundation.nova.runtime.network.updaters

import android.util.Log
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.EmptyScope
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.UpdateScope
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onEach

class BlockNumberUpdater(
    private val chainRegistry: ChainRegistry,
    private val storageCache: StorageCache
) : Updater<Chain> {

    override val scope: UpdateScope<Chain> = EmptyScope()

    override val requiredModules = listOf(Modules.SYSTEM)

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: Chain,
    ): Flow<Updater.SideEffect> {
        val runtime = chainRegistry.getRuntime(scopeValue.id)

        val storageKey = runCatching { storageKey(runtime) }.getOrNull() ?: return emptyFlow()

        return storageSubscriptionBuilder.subscribe(storageKey)
            .onEach {
                Log.d("BlockNumberUpdater", "Block number updated: ${it.value}")
                storageCache.insert(it, scopeValue.id)
            }
            .noSideAffects()
    }

    private fun storageKey(runtime: RuntimeSnapshot): String {
        return runtime.metadata.system().storage("Number").storageKey()
    }
}
