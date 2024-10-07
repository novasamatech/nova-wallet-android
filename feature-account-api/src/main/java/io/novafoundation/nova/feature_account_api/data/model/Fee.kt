package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

interface Fee {

    companion object

    val amount: BigInteger

    /**
     * Information about origin that is supposed to send the transaction fee was calculated against
     */
    val submissionOrigin: SubmissionOrigin

    val asset: Chain.Asset
}

data class EvmFee(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    override val submissionOrigin: SubmissionOrigin,
    override val asset: Chain.Asset
) : Fee {
    override val amount = gasLimit * gasPrice
}

class SubstrateFee(
    override val amount: BigInteger,
    override val submissionOrigin: SubmissionOrigin,
    override val asset: Chain.Asset
) : Fee

val Fee.requestedAccountPaysFees: Boolean
    get() = submissionOrigin.requestedOrigin.contentEquals(submissionOrigin.actualOrigin)

val Fee.amountByRequestedAccount: BigInteger
    get() = amount.asAmountByRequestedAccount

context(Fee)
val BigInteger.asAmountByRequestedAccount: BigInteger
    get() = if (requestedAccountPaysFees) {
        this
    } else {
        BigInteger.ZERO
    }

fun FeePaymentCurrency.toFeePaymentAsset(chain: Chain): Chain.Asset {
    return when (this) {
        is FeePaymentCurrency.Asset -> asset
        FeePaymentCurrency.Native -> chain.utilityAsset
    }
}
