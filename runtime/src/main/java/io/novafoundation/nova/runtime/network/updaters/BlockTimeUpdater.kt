package io.novafoundation.nova.runtime.network.updaters

import android.util.Log
import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

private data class BlockObservation(
    val blockNumber: BlockNumber,
    val timestamp: BigInteger,
)

/**
 * Samples the real block time of a chain by observing `(block number, timestamp)` pairs, see [observing] for the algorithm.
 * The result is persisted in [SampledBlockTimeStorage] and blended with chain constants in `ChainStateRepository.predictedBlockTime`.
 */
class BlockTimeUpdater(
    private val chainIdHolder: ChainIdHolder,
    private val chainRegistry: ChainRegistry,
    private val sampledBlockTimeStorage: SampledBlockTimeStorage,
    private val remoteStorageSource: StorageDataSource,
) : GlobalScopeUpdater {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder, scopeValue: Unit): Flow<Updater.SideEffect> {
        val chainId = chainIdHolder.chainId()
        val runtime = chainRegistry.getRuntime(chainId)
        val storage = runtime.metadata.system().storage("Number")

        val blockNumberKey = storage.storageKey()

        return storageSubscriptionBuilder.subscribe(blockNumberKey)
            .drop(1) // ignore fist subscription value since it comes immediately
            .map {
                val timestamp = remoteStorageSource.query(chainId, at = it.block) {
                    runtime.metadata.timestamp().storage("Now").query(binding = ::bindNumber)
                }

                val blockNumber = bindNumber(storage.decodeValue(it.value, runtime))

                BlockObservation(blockNumber = blockNumber, timestamp = timestamp)
            }
            .onEach { observation -> observe(chainId, observation) }
            .noSideAffects()
    }

    private suspend fun observe(chainId: ChainId, observation: BlockObservation) {
        val current = sampledBlockTimeStorage.get(chainId)
        val updated = current.observing(block = observation.blockNumber, timestampMillis = observation.timestamp)

        if (updated == current) return

        if (updated.sampleSize != current.sampleSize) {
            Log.d(LOG_TAG, "New block time sample on chain $chainId at block ${observation.blockNumber}: ${updated.averageBlockTime} ms")
        }

        sampledBlockTimeStorage.put(chainId, updated)
    }
}
