package io.novafoundation.nova.runtime.repository

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.util.BlockDurationEstimator

suspend fun ChainStateRepository.blockDurationEstimator(chainId: ChainId): BlockDurationEstimator {
    return BlockDurationEstimator(
        currentBlock = currentBlock(chainId),
        blockTimeMillis = predictedBlockTime(chainId)
    )
}
