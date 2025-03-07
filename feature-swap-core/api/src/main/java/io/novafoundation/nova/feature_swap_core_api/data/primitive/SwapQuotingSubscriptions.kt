package io.novafoundation.nova.feature_swap_core_api.data.primitive

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow

interface SwapQuotingSubscriptions {

    suspend fun blockNumber(chainId: ChainId): Flow<BlockNumber>
}
