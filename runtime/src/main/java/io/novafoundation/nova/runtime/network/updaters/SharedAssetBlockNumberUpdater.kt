package io.novafoundation.nova.runtime.network.updaters

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class SharedAssetBlockNumberUpdater(
    chainRegistry: ChainRegistry,
    chainIdHolder: ChainIdHolder,
    storageCache: StorageCache
) : SingleStorageKeyUpdater<Unit>(GlobalScope, chainIdHolder, chainRegistry, storageCache) {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String {
        return runtime.metadata.system().storage("Number").storageKey()
    }

    override val requiredModules = listOf(Modules.SYSTEM)
}
