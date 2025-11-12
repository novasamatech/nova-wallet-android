package io.novafoundation.nova.feature_gift_impl.presentation.amount.fee

import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.amount.FeeInspector
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class GiftFeeInspector : FeeInspector<GiftFee> {

    override fun inspectFeeAmount(fee: GiftFee): FeeInspector.InspectedFeeAmount {
        return FeeInspector.InspectedFeeAmount(
            checkedAgainstMinimumBalance = fee.amount,
            deductedFromTransferable = fee.amount
        )
    }

    override fun getSubmissionFeeAsset(fee: GiftFee): Chain.Asset {
        return fee.createGiftFee.asset
    }
}
