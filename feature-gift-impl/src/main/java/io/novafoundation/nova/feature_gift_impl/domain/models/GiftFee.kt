package io.novafoundation.nova.feature_gift_impl.domain.models

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.SubmissionFee
import io.novafoundation.nova.feature_account_api.data.model.getAmount
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

data class GiftFee(
    val createGiftFee: SubmissionFee,
    val claimGiftFee: SubmissionFee
) : FeeBase, MaxAvailableDeduction {

    fun replaceSubmission(newSubmissionFee: SubmissionFee): GiftFee {
        return copy(createGiftFee = newSubmissionFee)
    }

    override fun maxAmountDeductionFor(amountAsset: Chain.Asset): BigInteger {
        val createFeeAmount = createGiftFee.getAmount(amountAsset).orZero()
        val claimFeeAmount = claimGiftFee.getAmount(amountAsset).orZero()

        return createFeeAmount + claimFeeAmount
    }

    override val amount: BigInteger = createGiftFee.amount + claimGiftFee.amount

    override val asset: Chain.Asset = createGiftFee.asset
}
