package io.novafoundation.nova.runtime.extrinsic

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.lang.Integer.min

private const val FALLBACK_MAX_HASH_COUNT = 250
private const val MAX_FINALITY_LAG = 5
private const val MORTAL_PERIOD = 5 * 60 * 1000

class Mortality(val era: Era.Mortal, val blockHash: String)

class MortalityConstructor(
    private val rpcCalls: RpcCalls,
    private val chainStateRepository: ChainStateRepository,
) {

    fun mortalPeriodMillis(): Long = MORTAL_PERIOD.toLong()

    suspend fun constructMortality(chainId: ChainId): Mortality = withContext(Dispatchers.IO) {
        val finalizedHash = async { rpcCalls.getFinalizedHead(chainId) }

        val bestHeader = async { rpcCalls.getBlockHeader(chainId) }
        val finalizedHeader = async { rpcCalls.getBlockHeader(chainId, finalizedHash()) }

        val currentHeader = async { bestHeader().parentHash?.let { rpcCalls.getBlockHeader(chainId, it) } ?: bestHeader() }

        val currentNumber = currentHeader().number
        val finalizedNumber = finalizedHeader().number

        val finalityLag = (currentNumber - finalizedNumber).atLeastZero()

        val startBlockNumber = finalizedNumber

        val blockHashCount = chainStateRepository.blockHashCount(chainId)?.toInt()

        val blockTime = chainStateRepository.predictedBlockTime(chainId).toInt()

        val mortalPeriod = MORTAL_PERIOD / blockTime + finalityLag

        val unmappedPeriod = min(blockHashCount ?: FALLBACK_MAX_HASH_COUNT, mortalPeriod)

        val era = Era.getEraFromBlockPeriod(startBlockNumber, unmappedPeriod)
        val eraBlockNumber = ((startBlockNumber - era.phase) / era.period) * era.period + era.phase

        val eraBlockHash = rpcCalls.getBlockHash(chainId, eraBlockNumber.toBigInteger())

        Mortality(era, eraBlockHash)
    }
}
