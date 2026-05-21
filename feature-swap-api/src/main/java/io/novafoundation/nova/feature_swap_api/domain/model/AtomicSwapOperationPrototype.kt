package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
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

    /**
     * Final-segment hook for adjusting the displayed amountOut. Default is identity.
     * Operations that levy a per-operation fee on the user-visible output (e.g. Hydration
     * commission) override this to return the post-fee amount, so the quote layer stays
     * source-agnostic.
     */
    suspend fun postProcessFinalAmountOut(amountOut: Balance): Balance = amountOut
}

interface UsdConverter {

    suspend fun nativeAssetEquivalentOf(usdAmount: Double): BigDecimal
}
