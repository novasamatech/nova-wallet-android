package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigDecimal

interface AtomicSwapOperationPrototype {

    val fromChain: ChainId

    /**
     * Roughly estimate fees for the current operation in any asset
     * Implementations should favour speed instead of precision as this is called for each quoting action
     */
    suspend fun roughlyEstimateNativeFee(usdConverter: UsdConverter): BigDecimal
}

interface UsdConverter {

    suspend fun nativeAssetEquivalentOf(usdAmount: Double): BigDecimal
}
