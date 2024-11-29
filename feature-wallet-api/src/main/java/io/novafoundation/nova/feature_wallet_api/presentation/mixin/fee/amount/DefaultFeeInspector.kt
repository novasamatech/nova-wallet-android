package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount

import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class DefaultFeeInspector<F : SubmissionFee> : FeeInspector<F> {

    override fun inspectFeeAmount(fee: F): FeeInspector.InspectedFeeAmount {
        val amount = fee.amountByExecutingAccount

        return FeeInspector.InspectedFeeAmount(
            checkedAgainstMinimumBalance = amount,
            deductedFromTransferable = amount
        )
    }

    override fun getSubmissionFeeAsset(fee: F): Chain.Asset {
        return fee.asset
    }
}
