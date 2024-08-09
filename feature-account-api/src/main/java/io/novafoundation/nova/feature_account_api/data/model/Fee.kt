package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger

sealed interface Fee {

    companion object

    val amount: BigInteger

    /**
     * Information about origin that is supposed to send the transaction fee was calculated against
     */
    val submissionOrigin: SubmissionOrigin
}

data class EvmFee(
    val gasLimit: BigInteger,
    val gasPrice: BigInteger,
    override val submissionOrigin: SubmissionOrigin
) : Fee {
    override val amount = gasLimit * gasPrice
}

interface FeeInAsset : Fee {
    val assetId: FullChainAssetId
}

class SubstrateFee(
    override val amount: BigInteger,
    override val submissionOrigin: SubmissionOrigin,
    override val assetId: FullChainAssetId
) : FeeInAsset

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
