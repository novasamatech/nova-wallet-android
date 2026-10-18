package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.TransferMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.nativeTransfer
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder

/** Used unchanged for estimation and submission; ExtrinsicService batches calls atomically. */
class BittensorSubnetStakeOperation(
    val quote: BittensorSubnetBuyQuote,
    val hotkey: AccountIdKey,
    val beneficiary: AccountIdKey
) {

    init {
        require(hotkey.value.size == 32 && beneficiary.value.size == 32)
        require(quote.serviceFeeTaoInPlanks.isNonNegative && quote.stakeTaoInPlanks.isPositive())
        require(quote.simulation.amountIn + quote.simulation.taoFee == quote.stakeTaoInPlanks) {
            "Partial subnet quotes are not supported"
        }
    }

    fun build(builder: ExtrinsicBuilder) = with(builder) {
        addBittensorSubnetStake(quote.subnetId, hotkey.value, quote.stakeTaoInPlanks, quote.limitPrice)
        if (quote.serviceFeeTaoInPlanks.isPositive()) {
            nativeTransfer(beneficiary.value, quote.serviceFeeTaoInPlanks, TransferMode.KEEP_ALIVE)
        }
        Unit
    }
}
