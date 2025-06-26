package io.novafoundation.nova.runtime.multiNetwork.runtime

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.ext.typesUsage
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.TypesUsage
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

class RuntimeProvider(
    private val runtimeFactory: RuntimeFactory,
    private val runtimeSyncService: RuntimeSyncService,
    private val baseTypeSynchronizer: BaseTypeSynchronizer,
    private val runtimeFilesCache: RuntimeFilesCache,
    chain: Chain,
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val chainId = chain.id

    private var typesUsage = chain.typesUsage

    private val runtimeFlow = MutableSharedFlow<ConstructedRuntime>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var currentConstructionJob: Job? = null

    suspend fun get(): RuntimeSnapshot {
        val runtime = runtimeFlow.first()

        return runtime.runtime
    }

    suspend fun getRaw(): RawRuntimeMetadata {
        return runtimeFilesCache.getChainMetadata(chainId)
    }

    fun observe(): Flow<RuntimeSnapshot> = runtimeFlow.map { it.runtime }

    init {
        baseTypeSynchronizer.syncStatusFlow
            .onEach(::considerReconstructingRuntime)
            .launchIn(this)

        runtimeSyncService.syncResultFlow(chainId)
            .onEach(::considerReconstructingRuntime)
            .launchIn(this)

        tryLoadFromCache()
    }

    fun finish() {
        invalidateRuntime()

        cancel()
    }

    fun considerUpdatingTypesUsage(newTypesUsage: TypesUsage) {
        if (typesUsage != newTypesUsage) {
            typesUsage = newTypesUsage

            constructNewRuntime(typesUsage)
        }
    }

    private fun tryLoadFromCache() {
        constructNewRuntime(typesUsage)
    }

    private fun considerReconstructingRuntime(runtimeSyncResult: SyncResult) {
        launch {
            currentConstructionJob?.join()

            val currentVersion = runtimeFlow.replayCache.firstOrNull()

            if (
                currentVersion == null ||
                // metadata was synced and new hash is different from current one
                (runtimeSyncResult.metadataHash != null && currentVersion.metadataHash != runtimeSyncResult.metadataHash) ||
                // types were synced and new hash is different from current one
                (runtimeSyncResult.typesHash != null && currentVersion.ownTypesHash != runtimeSyncResult.typesHash)
            ) {
                constructNewRuntime(typesUsage)
            }
        }
    }

    private fun considerReconstructingRuntime(newBaseTypesHash: String) {
        launch {
            currentConstructionJob?.join()

            val currentVersion = runtimeFlow.replayCache.firstOrNull()

            if (typesUsage == TypesUsage.OWN) {
                return@launch
            }

            if (
                currentVersion == null ||
                currentVersion.baseTypesHash != newBaseTypesHash
            ) {
                constructNewRuntime(typesUsage)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun constructNewRuntime(typesUsage: TypesUsage) {
        currentConstructionJob?.cancel()

        currentConstructionJob = launch {
            invalidateRuntime()

            runCatching {
                val (value, duration) = measureTimedValue { runtimeFactory.constructRuntime(chainId, typesUsage) }

                Log.d(this@RuntimeProvider.LOG_TAG, "Constructed runtime for $chainId in ${duration.inWholeSeconds} seconds")

                runtimeFlow.emit(value)
            }.onFailure {
                when (it) {
                    ChainInfoNotInCacheException -> {
                        runtimeSyncService.cacheNotFound(chainId)

                        Log.w(this@RuntimeProvider.LOG_TAG, "Runtime cache was not found for $chainId")
                    }
                    BaseTypesNotInCacheException -> {
                        baseTypeSynchronizer.cacheNotFound()

                        Log.w(this@RuntimeProvider.LOG_TAG, "Base types cache were not found")
                    }
                    NoRuntimeVersionException -> {
                        Log.w(this@RuntimeProvider.LOG_TAG, "Runtime version for $chainId was not found in database")
                    } // pass
                    else -> Log.e(this@RuntimeProvider.LOG_TAG, "Failed to construct runtime ($chainId)", it)
                }
            }

            currentConstructionJob = null
        }
    }

    private fun invalidateRuntime() {
        runtimeFlow.resetReplayCache()
    }
}
