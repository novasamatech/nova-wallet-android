package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindBlockNumber
import io.novafoundation.nova.common.utils.babe
import io.novafoundation.nova.common.utils.babeOrNull
import io.novafoundation.nova.common.utils.isParachain
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.common.utils.optionalNumberConstant
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.common.utils.timestampOrNull
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.observeNonNull
import io.novafoundation.nova.runtime.storage.source.queryNonNull
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage
import jp.co.soramitsu.fearless_utils.runtime.metadata.storageKey
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

private val FALLBACK_BLOCK_TIME_MILLIS_RELAYCHAIN = (6 * 1000).toBigInteger()
private val FALLBACK_BLOCK_TIME_MILLIS_PARACHAIN = 2.toBigInteger() * FALLBACK_BLOCK_TIME_MILLIS_RELAYCHAIN

private val PERIOD_VALIDITY_THRESHOLD = 500.toBigInteger()

class ChainStateRepository(
    private val localStorage: StorageDataSource,
    private val chainRegistry: ChainRegistry
) {

    suspend fun expectedBlockTimeInMillis(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)

        return runtime.metadata.babe().numberConstant("ExpectedBlockTime", runtime)
    }

    suspend fun predictedBlockTime(chainId: ChainId): BigInteger {
        val runtime = chainRegistry.getRuntime(chainId)
        val metadata = runtime.metadata


        return metadata.babeOrNull()?.numberConstant("ExpectedBlockTime", runtime)
            // Some chains incorrectly use these, i.e. it is set to values such as 0 or even 2
            // Use a low minimum validity threshold to check these against
            ?: metadata.timestampOrNull()?.numberConstant("MinimumPeriod", runtime)?.takeIf { it > PERIOD_VALIDITY_THRESHOLD }
            ?: fallbackBlockTime(runtime)
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

    fun currentBlockNumberFlow(chainId: ChainId): Flow<BlockNumber> = localStorage.observeNonNull(
        keyBuilder = ::currentBlockStorageKey,
        binding = { scale, runtime -> bindBlockNumber(scale, runtime) },
        chainId = chainId
    )

    private fun currentBlockStorageKey(runtime: RuntimeSnapshot) = runtime.metadata.system().storage("Number").storageKey()

    private fun fallbackBlockTime(runtime: RuntimeSnapshot): BigInteger {
        return if (runtime.isParachain()) {
            FALLBACK_BLOCK_TIME_MILLIS_PARACHAIN
        } else {
            FALLBACK_BLOCK_TIME_MILLIS_RELAYCHAIN
        }
    }
}
