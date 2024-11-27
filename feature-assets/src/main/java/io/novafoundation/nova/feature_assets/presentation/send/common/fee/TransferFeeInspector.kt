package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import io.novafoundation.nova.feature_account_api.data.model.amountByExecutingAccount
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TransferFeeInspector : FeeInspector<TransferFee> {

    override fun getSubmissionFeeAsset(fee: TransferFee): Chain.Asset {
        return fee.originFee.submissionFee.asset
    }

    override fun inspectFeeAmount(fee: TransferFee): FeeInspector.InspectedFeeAmount {
        val submissionFee = fee.originFee.submissionFee

        return FeeInspector.InspectedFeeAmount(
            deductedFromTransferable = fee.totalFeeByExecutingAccount(submissionFee.asset),
            checkedAgainstMinimumBalance = submissionFee.amountByExecutingAccount,
        )
    }
}
