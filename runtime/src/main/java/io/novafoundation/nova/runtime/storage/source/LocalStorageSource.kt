package io.novafoundation.nova.runtime.storage.source

import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.SubstrateSubscriptionBuilder
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.query.LocalStorageQueryContext
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalStorageSource(
    chainRegistry: ChainRegistry,
    sharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
    private val storageCache: StorageCache,
) : BaseStorageSource(chainRegistry, sharedRequestsBuilderFactory) {

    override suspend fun query(key: String, chainId: String, at: BlockHash?): String? {
        requireWithoutAt(at)

        return storageCache.getEntry(key, chainId).content
    }

    override suspend fun observe(key: String, chainId: String): Flow<String?> {
        return storageCache.observeEntry(key, chainId)
            .map { it.content }
    }

    override suspend fun queryChildState(storageKey: String, childKey: String, chainId: String): String? {
        throw NotImplementedError("Child state queries are not yet supported in local storage")
    }

    override suspend fun createQueryContext(
        chainId: String,
        at: BlockHash?,
        runtime: RuntimeSnapshot,
        subscriptionBuilder: SubstrateSubscriptionBuilder?
    ): StorageQueryContext {
        return LocalStorageQueryContext(storageCache, chainId, at, runtime)
    }

    private fun requireWithoutAt(at: BlockHash?) = require(at == null) {
        "`At` parameter is not supported in local storage"
    }
}
