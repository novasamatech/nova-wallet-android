package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.util.BlockDurationEstimator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

suspend fun ChainStateRepository.blockDurationEstimator(chainId: ChainId): BlockDurationEstimator {
    return BlockDurationEstimator(
        currentBlock = currentBlock(chainId),
        blockTimeMillis = predictedBlockTime(chainId)
    )
}

fun ChainStateRepository.blockDurationEstimatorFlow(chainId: ChainId): Flow<BlockDurationEstimator> {
    return combine(currentBlockNumberFlow(chainId), predictedBlockTimeFlow(chainId)) { currentBlock, blockTime ->
        BlockDurationEstimator(currentBlock, blockTime)
    }
}
