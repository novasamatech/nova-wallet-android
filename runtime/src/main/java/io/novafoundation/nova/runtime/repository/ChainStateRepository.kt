package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.utils.babeOrNull
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.isParachain
import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.optionalNumberConstant
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.common.utils.timestampOrNull
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.updaters.SampledBlockTime
import io.novafoundation.nova.runtime.storage.SampledBlockTimeStorage
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.observeNonNull
import io.novafoundation.nova.runtime.storage.source.query.api.observeNonNull
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import io.novafoundation.nova.runtime.storage.typed.number
import io.novafoundation.nova.runtime.storage.typed.system
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val FALLBACK_BLOCK_TIME_MILLIS_RELAYCHAIN = (6 * 1000).toBigInteger()
private val FALLBACK_BLOCK_TIME_MILLIS_PARACHAIN = 2.toBigInteger() * FALLBACK_BLOCK_TIME_MILLIS_RELAYCHAIN

private val PERIOD_VALIDITY_THRESHOLD = 100.toBigInteger()

private val REQUIRED_SAMPLED_BLOCKS = 10.toBigInteger()

class ChainStateRepository(
    private val localStorage: StorageDataSource,
    private val remoteStorage: StorageDataSource,
    private val sampledBlockTimeStorage: SampledBlockTimeStorage,
    private val chainRegistry: ChainRegistry
) {

    suspend fun expectedBlockTimeInMillis(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)
        val chain = chainRegistry.getChain(chainId)

        return blockTimeFromConstants(chain, runtime)
    }

    suspend fun predictedBlockTime(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)
        val chain = chainRegistry.getChain(chainId)

        val blockTimeFromConstants = blockTimeFromConstants(chain, runtime)
        val sampledBlockTime = sampledBlockTimeStorage.get(chainId)

        return weightedAverageBlockTime(sampledBlockTime, blockTimeFromConstants)
    }

    fun predictedBlockTimeFlow(chainId: ChainId): Flow<BigInteger> {
        return flowOfAll {
            val runtime = chainRegistry.getRuntime(chainId)
            val chain = chainRegistry.getChain(chainId)

            val blockTimeFromConstants = blockTimeFromConstants(chain, runtime)

            sampledBlockTimeStorage.observe(chainId).map {
                weightedAverageBlockTime(it, blockTimeFromConstants)
            }
        }
    }

    private fun weightedAverageBlockTime(
        sampledBlockTime: SampledBlockTime,
        blockTimeFromConstants: BigInteger
    ): BigInteger {
        val cappedSampleSize = sampledBlockTime.sampleSize.min(REQUIRED_SAMPLED_BLOCKS)
        val sampledPart = cappedSampleSize * sampledBlockTime.averageBlockTime
        val constantsPart = (REQUIRED_SAMPLED_BLOCKS - cappedSampleSize) * blockTimeFromConstants

        return (sampledPart + constantsPart) / REQUIRED_SAMPLED_BLOCKS
    }

    private fun blockTimeFromConstants(chain: Chain, runtime: RuntimeSnapshot): BigInteger {
        return chain.additional?.defaultBlockTimeMillis?.toBigInteger()
            ?: runtime.metadata.babeOrNull()?.numberConstant("ExpectedBlockTime", runtime)
            // Some chains incorrectly use these, i.e. it is set to values such as 0 or even 2
            // Use a low minimum validity threshold to check these against
            ?: blockTimeFromTimestampPallet(runtime)
            ?: fallbackBlockTime(runtime)
    }

    private fun blockTimeFromTimestampPallet(runtime: RuntimeSnapshot): BigInteger? {
        val blockTime = runtime.metadata.timestampOrNull()?.numberConstant("MinimumPeriod", runtime)?.takeIf { it > PERIOD_VALIDITY_THRESHOLD }
            ?: return null

        return blockTime * 2.toBigInteger()
    }

    suspend fun blockHashCount(chainId: ChainId): BigInteger? {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.system().optionalNumberConstant("BlockHashCount", runtime)
    }

    suspend fun currentBlock(chainId: ChainId) = localStorage.queryNonNull(
        keyBuilder = ::currentBlockStorageKey,
        binding = { scale, runtime -> bindBlockNumber(scale, runtime) },
        chainId = chainId
    )

    fun currentBlockNumberFlow(chainId: ChainId): Flow<BlockNumber> = localStorage.observeBlockNumber(chainId)

    fun currentRemoteBlockNumberFlow(chainId: ChainId): Flow<BlockNumber> = remoteStorage.observeBlockNumber(chainId)

    private fun StorageDataSource.observeBlockNumber(chainId: ChainId) = subscribe(chainId) {
        metadata.system.number.observeNonNull()
    }

    private fun currentBlockStorageKey(runtime: RuntimeSnapshot) = runtime.metadata.system().storage("Number").storageKey()

    private fun fallbackBlockTime(runtime: RuntimeSnapshot): BigInteger {
        return if (runtime.isParachain()) {
            FALLBACK_BLOCK_TIME_MILLIS_PARACHAIN
        } else {
            FALLBACK_BLOCK_TIME_MILLIS_RELAYCHAIN
        }
    }
}

suspend fun ChainStateRepository.expectedBlockTime(chainId: ChainId): Duration {
    return expectedBlockTimeInMillis(chainId).toLong().milliseconds
}
