package io.novafoundation.nova.feature_swap_impl.data.assetExchange.xcm

import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperation
import io.novafoundation.nova.feature_swap_api.domain.model.AtomicSwapOperationArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapGraphEdge
import io.novafoundation.nova.feature_swap_api.domain.model.SwapLimit
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface XcmAppendableOperation: AtomicSwapOperation {

    fun withdrawAmount(): Balance

    override suspend fun appendSegment(edge: SwapGraphEdge, args: AtomicSwapOperationArgs): XcmAppendableOperation?

    /**
     * Append self to the ongoing xcm program
     *
     * Implementation should assume the following:
     * 1. Relevant asset withdrawal is already performed based on output of [withdrawAmount]
     * 2. Fees are already been paid
     * 3. Deposit (if this is a final operation) will be appended after this method completes
     */
    suspend fun appendTo(xcmBuilder: SwapXcmBuilder, actualSwapLimit: SwapLimit)
}
c
