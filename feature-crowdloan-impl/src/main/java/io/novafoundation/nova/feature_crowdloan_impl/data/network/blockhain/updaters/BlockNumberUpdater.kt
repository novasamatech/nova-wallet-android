package io.novafoundation.nova.feature_crowdloan_impl.data.network.blockhain.updaters

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class BlockNumberUpdater(
    chainRegistry: ChainRegistry,
    crowdloanSharedState: CrowdloanSharedState,
    storageCache: StorageCache
) : SingleStorageKeyUpdater<GlobalScope>(GlobalScope, crowdloanSharedState, chainRegistry, storageCache) {

    override suspend fun storageKey(runtime: RuntimeSnapshot): String {
        return runtime.metadata.system().storage("Number").storageKey()
    }

    override val requiredModules = listOf(Modules.SYSTEM)
}
