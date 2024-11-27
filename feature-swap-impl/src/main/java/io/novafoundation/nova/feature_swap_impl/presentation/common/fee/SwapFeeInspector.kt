package io.novafoundation.nova.feature_swap_impl.presentation.common.fee

import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector.InspectedFeeAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class SwapFeeInspector : FeeInspector<SwapFee> {

    override fun inspectFeeAmount(fee: SwapFee): InspectedFeeAmount {
        return InspectedFeeAmount(
            checkedAgainstMinimumBalance = fee.initialSubmissionFee.amountByExecutingAccount,
            deductedFromTransferable = fee.maxAmountDeductionFor(fee.initialSubmissionFee.asset)
        )
    }

    override fun getSubmissionFeeAsset(fee: SwapFee): Chain.Asset {
        return fee.initialSubmissionFee.asset
    }
}
