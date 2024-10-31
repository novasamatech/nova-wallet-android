package io.novafoundation.nova.feature_swap_impl.presentation.common.fee

import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeBalanceExtractor
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapFeeBalanceExtractor : FeeBalanceExtractor<SwapFee> {

    override fun requiredBalanceToPayFee(fee: SwapFee, chainAsset: Chain.Asset): Balance {
        return fee.maxAmountDeductionFor(chainAsset)
    }
}
