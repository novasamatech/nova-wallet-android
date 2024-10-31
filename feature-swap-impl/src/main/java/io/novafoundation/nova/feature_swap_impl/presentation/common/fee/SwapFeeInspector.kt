package io.novafoundation.nova.feature_swap_impl.presentation.common.fee

import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapFeeInspector : FeeInspector<SwapFee> {

    override fun requiredBalanceToPayFee(fee: SwapFee, chainAsset: Chain.Asset): Balance {
        return fee.maxAmountDeductionFor(chainAsset)
    }

    override fun getSubmissionFeeAsset(fee: SwapFee): Chain.Asset {
        return fee.asset
    }
}
