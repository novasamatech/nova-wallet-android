package io.novafoundation.nova.feature_wallet_api.domain.model

import io.novafoundation.nova.common.utils.emptySubstrateAccountId
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.amountByRequestedAccount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleGenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import java.math.BigInteger

typealias CrossChainGenericFee = SimpleGenericFee<CrossChainFee>

typealias CrossChainDecimalFee = GenericDecimalFee<CrossChainGenericFee>

class CrossChainFee(
    val senderPart: BigInteger,
    val holdingPart: BigInteger,
    override val submissionOrigin: SubmissionOrigin
) : Fee {

    override val amount: BigInteger = senderPart + holdingPart

    companion object
}

fun CrossChainFee.Companion.stub() = CrossChainFee(
    senderPart = BigInteger.ZERO,
    holdingPart = BigInteger.ZERO,
    submissionOrigin = SubmissionOrigin(emptySubstrateAccountId(), emptySubstrateAccountId())
)

val CrossChainFee.holdingPartByRequestedAccount
    get() = holdingPart.amountByRequestedAccount

val CrossChainFee.senderPartByRequestedAccount
    get() = senderPart.amountByRequestedAccount
