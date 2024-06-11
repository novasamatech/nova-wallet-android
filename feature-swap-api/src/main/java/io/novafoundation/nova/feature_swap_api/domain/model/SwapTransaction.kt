package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface SwapTransaction {

    suspend fun estimateFee(): SwapTransactionFee

    suspend fun submit(): Result<*>
}

class SwapTransactionArgs(
    val swapLimit: SwapLimit,
    val customFeeAsset: Chain.Asset?,
    val nativeAsset: Asset,
)

class SwapTransactionFee(
    val networkFee: Fee,
    val minimumBalanceBuyIn: MinimumBalanceBuyIn
)
