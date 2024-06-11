package io.novafoundation.nova.runtime.multiNetwork.runtime

import android.util.Log
import io.novafoundation.nova.common.utils.md5
import io.novafoundation.nova.common.utils.retryUntilDone
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.TypesFetcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.concurrent.ConcurrentHashMap

data class SyncInfo(
    val connection: ChainConnection,
    val typesUrl: String?,
)

class SyncResult(
    val chainId: String,
    val metadataHash: FileHash?,
    val typesHash: FileHash?,
)

private const val LOG_TAG = "RuntimeSyncService"

class RuntimeSyncService(
    private val typesFetcher: TypesFetcher,
    private val runtimeFilesCache: RuntimeFilesCache,
    private val chainDao: ChainDao,
    private val runtimeMetadataFetcher: RuntimeMetadataFetcher,
    private val cacheMigrator: RuntimeCacheMigrator,
    private val chainSyncDispatcher: ChainSyncDispatcher,
) {

    private val knownChains = ConcurrentHashMap<String, SyncInfo>()

    private val _syncStatusFlow = MutableSharedFlow<SyncResult>()

    fun syncResultFlow(forChain: String): Flow<SyncResult> {
        return _syncStatusFlow.filter { it.chainId == forChain }
    }

    fun applyRuntimeVersion(chainId: String) {
        launchSync(chainId)
    }

    fun registerChain(chain: Chain, connection: ChainConnection) {
        val existingSyncInfo = knownChains[chain.id]

        val newSyncInfo = SyncInfo(
            connection = connection,
            typesUrl = chain.types?.url
        )

        knownChains[chain.id] = newSyncInfo

        if (existingSyncInfo != null && existingSyncInfo != newSyncInfo) {
            launchSync(chain.id)
        }
    }

    fun unregisterChain(chainId: String) {
        knownChains.remove(chainId)

        chainSyncDispatcher.cancelExistingSync(chainId)
    }

    // Android may clear cache files sometimes so it necessary to have force sync mechanism
    fun cacheNotFound(chainId: String) {
        if (!chainSyncDispatcher.isSyncing(chainId)) {
            launchSync(chainId, forceFullSync = true)
        }
    }

    fun isSyncing(chainId: String): Boolean {
        return chainSyncDispatcher.isSyncing(chainId)
    }

    private fun launchSync(
        chainId: String,
        forceFullSync: Boolean = false,
    ) {
        chainSyncDispatcher.cancelExistingSync(chainId)

        chainSyncDispatcher.launchSync(chainId) {
            val syncResult = runCatching {
                sync(chainId, forceFullSync)
            }.getOrNull()

            chainSyncDispatcher.syncFinished(chainId)

            syncResult?.let { _syncStatusFlow.emit(it) }
        }
    }

    private suspend fun sync(
        chainId: String,
        forceFullSync: Boolean,
    ): SyncResult? {
        val syncInfo = knownChains[chainId]

        if (syncInfo == null) {
            Log.w(LOG_TAG, "Unknown chain with id $chainId requested to be synced")
            return null
        }

        val runtimeInfo = chainDao.runtimeInfo(chainId) ?: return null

        val shouldSyncMetadata = runtimeInfo.shouldSyncMetadata() || forceFullSync

        val metadataHash = if (shouldSyncMetadata) {
            val runtimeMetadata = runtimeMetadataFetcher.fetchRawMetadata(chainId, syncInfo.connection.socketService)

            runtimeFilesCache.saveChainMetadata(chainId, runtimeMetadata)

            chainDao.updateSyncedRuntimeVersion(chainId, runtimeInfo.remoteVersion, cacheMigrator.latestVersion())

            runtimeMetadata.metadataContent.md5()
        } else {
            null
        }

        val typesHash = syncInfo.typesUrl?.let { typesUrl ->
            retryUntilDone {
                val types = typesFetcher.getTypes(typesUrl)

                runtimeFilesCache.saveChainTypes(chainId, types)

                types.md5()
            }
        }

        return SyncResult(
            metadataHash = metadataHash,
            typesHash = typesHash,
            chainId = chainId
        )
    }

    private fun ChainRuntimeInfoLocal.shouldSyncMetadata() = syncedVersion != remoteVersion || cacheMigrator.needsMetadataFetch(localMigratorVersion)
}
