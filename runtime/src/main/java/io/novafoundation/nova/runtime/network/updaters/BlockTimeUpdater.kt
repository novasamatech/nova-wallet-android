package io.novafoundation.nova.runtime.network.updaters

import android.util.Log
import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.data.network.runtime.binding.BlockHash
import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.decodeValue
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.common.utils.zipWithPrevious
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

data class SampledBlockTime(
    val sampleSize: BigInteger,
    val averageBlockTime: BigInteger,
)

private data class BlockTimeUpdate(
    val at: BlockHash,
    val blockNumber: BlockNumber,
    val timestamp: BigInteger,
)

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

                BlockTimeUpdate(at = it.block, blockNumber = blockNumber, timestamp = timestamp)
            }
            .zipWithPrevious()
            .filter { (previous, current) ->
                previous != null && current.blockNumber - previous.blockNumber == BigInteger.ONE
            }
            .onEach { (previousUpdate, currentUpdate) ->
                val blockTime = currentUpdate.timestamp - previousUpdate!!.timestamp

                updateSampledBlockTime(chainId, blockTime)
            }.noSideAffects()
    }

    private suspend fun updateSampledBlockTime(chainId: ChainId, newSampledTime: BigInteger) {
        val current = sampledBlockTimeStorage.get(chainId)

        val adjustedSampleSize = current.sampleSize + BigInteger.ONE
        val adjustedAverage = (current.averageBlockTime * current.sampleSize + newSampledTime) / adjustedSampleSize
        val adjustedSampledBlockTime = SampledBlockTime(
            sampleSize = adjustedSampleSize,
            averageBlockTime = adjustedAverage
        )

        Log.d(LOG_TAG, "New block time update: $newSampledTime, adjustedAverage: $adjustedSampledBlockTime")

        sampledBlockTimeStorage.put(chainId, adjustedSampledBlockTime)
    }
}
