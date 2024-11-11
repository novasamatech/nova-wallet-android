package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigDecimal
import kotlin.time.Duration

interface AtomicSwapOperationPrototype {

    val fromChain: ChainId

    /**
     * Roughly estimate fees for the current operation in native asset
     * Implementations should favour speed instead of precision as this is called for each quoting action
     */
    suspend fun roughlyEstimateNativeFee(usdConverter: UsdConverter): BigDecimal

    suspend fun maximumExecutionTime(): Duration
}

interface UsdConverter {

    suspend fun nativeAssetEquivalentOf(usdAmount: Double): BigDecimal
}
