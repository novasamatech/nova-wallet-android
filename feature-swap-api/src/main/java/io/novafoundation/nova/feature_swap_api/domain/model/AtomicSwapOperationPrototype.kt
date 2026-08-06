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
     * Whether this operation levies a Nova service fee on its output. The quote layer applies the
     * fee to the **last** operation that charges it (see [serviceCommissionToAddOnTop]/[serviceCommissionIncludedIn]),
     * keeping the layer source-agnostic. Default: false.
     */
    val chargesServiceFee: Boolean
        get() = false

    /**
     * Extra output the operation levies on top of a net amount (SPECIFIED_OUT gross-up).
     * `gross = net + serviceFeeToAddOnTop(net)`. Default: no fee.
     */
    fun serviceCommissionToAddOnTop(net: Balance): Balance = Balance.ZERO

    /**
     * Service fee portion embedded within a gross amount (SPECIFIED_IN reduction).
     * `net = gross - serviceFeeIncludedIn(gross)`. Default: no fee.
     */
    fun serviceCommissionIncludedIn(gross: Balance): Balance = Balance.ZERO
}

interface UsdConverter {

    suspend fun nativeAssetEquivalentOf(usdAmount: Double): BigDecimal
}
