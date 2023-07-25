package io.novafoundation.nova.runtime.network.updaters

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.balances
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey

class TotalIssuanceUpdater(
    chainIdHolder: ChainIdHolder,
    storageCache: StorageCache,
    chainRegistry: ChainRegistry
) : SingleStorageKeyUpdater<Unit>(GlobalScope, chainIdHolder, chainRegistry, storageCache) {

    override val requiredModules: List<String> = listOf(Modules.BALANCES)

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String {
        return runtime.metadata.balances().storage("TotalIssuance").storageKey()
    }
}
