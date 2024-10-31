package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TransferFeeInspector : FeeInspector<TransferFee> {

    override fun requiredBalanceToPayFee(fee: TransferFee, chainAsset: Chain.Asset): Balance {
        return fee.totalFeeByExecutingAccount(chainAsset)
    }

    override fun getSubmissionFeeAsset(fee: TransferFee): Chain.Asset {
        return fee.originFee.submissionFee.asset
    }
}
