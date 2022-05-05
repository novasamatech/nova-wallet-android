package io.novafoundation.nova.runtime.network.updaters

import android.util.Log
import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.common.utils.timestamp
import io.novafoundation.nova.common.utils.zipWithPrevious
import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.math.BigInteger

data class SampledBlockTime(
    val sampleSize: BigInteger,
    val averageBlockTime: BigInteger,
)

class BlockTimeUpdater(
    private val chainIdHolder: ChainIdHolder,
    private val chainRegistry: ChainRegistry,
    private val sampledBlockTimeStorage: SampledBlockTimeStorage,
    private val remoteStorageSource: StorageDataSource,
) : GlobalScopeUpdater {

    override val requiredModules: List<String> = emptyList()

    override suspend fun listenForUpdates(storageSubscriptionBuilder: SubscriptionBuilder): Flow<Updater.SideEffect> {
        val chainId = chainIdHolder.chainId()
        val runtime = chainRegistry.getRuntime(chainId)
        val storage = runtime.metadata.system().storage("Number")

        val blockNumberKey = storage.storageKey()

        return storageSubscriptionBuilder.subscribe(blockNumberKey)
            .drop(1)
            .distinctUntilChangedBy { it.value }
            .map {
                remoteStorageSource.query(chainId, at = it.block) {
                    runtime.metadata.timestamp().storage("Now").query(binding = ::bindNumber)
                }
            }
            .zipWithPrevious()
            .onEach { (previousTime, currentTime) ->
                if (previousTime != null) {
                    val blockTime = currentTime - previousTime

                    updateSampledBlockTime(chainId, blockTime)
                }
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

        Log.d(LOG_TAG, "Adding new sampled time ($newSampledTime): $adjustedSampledBlockTime in $chainId")

        sampledBlockTimeStorage.put(chainId, adjustedSampledBlockTime)
    }
}
