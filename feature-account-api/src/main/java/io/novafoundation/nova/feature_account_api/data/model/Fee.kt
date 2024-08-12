package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isCommissionAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger

interface Fee {

    companion object

    val amount: BigInteger

    /**
     * Information about origin that is supposed to send the transaction fee was calculated against
     */
    val submissionOrigin: SubmissionOrigin

    val paymentAsset: PaymentAsset

    sealed interface PaymentAsset {

        object Native : PaymentAsset

        class Asset(val assetId: FullChainAssetId) : PaymentAsset
    }
}

data class EvmFee(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    override val submissionOrigin: SubmissionOrigin,
    override val paymentAsset: Fee.PaymentAsset
) : Fee {
    override val amount = gasLimit * gasPrice
}

class SubstrateFee(
    override val amount: BigInteger,
    override val submissionOrigin: SubmissionOrigin,
    override val paymentAsset: Fee.PaymentAsset
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

fun FeePaymentCurrency.toFeePaymentAsset(): Fee.PaymentAsset {
    return when (this) {
        is FeePaymentCurrency.Asset -> Fee.PaymentAsset.Asset(asset.fullId)
        FeePaymentCurrency.Native -> Fee.PaymentAsset.Native
    }
}

fun Chain.Asset.toFeePaymentAsset(): Fee.PaymentAsset {
    return when {
        this.isCommissionAsset -> Fee.PaymentAsset.Native
        else -> Fee.PaymentAsset.Asset(this.fullId)
    }
}
