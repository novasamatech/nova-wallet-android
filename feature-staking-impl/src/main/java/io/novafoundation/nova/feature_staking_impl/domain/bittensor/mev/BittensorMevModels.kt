package io.novafoundation.nova.feature_staking_impl.domain.bittensor.mev

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.runtime.extrinsic.Mortality
import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigInteger

class BittensorMevQuote(val innerFee: Fee, val carrierFee: Fee) {

    val totalFee: Fee = SubstrateFee(innerFee.amount + carrierFee.amount, innerFee.submissionOrigin, innerFee.asset)

    init {
        require(innerFee.amount.isNonNegative && carrierFee.amount.isNonNegative)
    }
}

sealed interface BittensorMevAvailability {

    class Available(val quote: BittensorMevQuote) : BittensorMevAvailability

    class Unavailable(val reason: BittensorMevUnavailableReason) : BittensorMevAvailability

    data object Disabled : BittensorMevAvailability
}

enum class BittensorMevUnavailableReason {
    UNSUPPORTED_WALLET, KEY_UNAVAILABLE, PAYLOAD_TOO_LARGE, INSUFFICIENT_NATIVE_BALANCE, QUOTES_FAILED
}

data class BittensorMevFunds(val transferable: BigInteger, val existentialDeposit: BigInteger) {

    init {
        require(transferable.isNonNegative && existentialDeposit.isNonNegative)
    }

    fun canPay(quote: BittensorMevQuote): Boolean = transferable >= quote.totalFee.amount + existentialDeposit
}

class BittensorMevTiming(val nonce: BigInteger, val birth: Int, val mortality: Mortality)

/** Absence of an inner is proven only after every canonical block in its era was inspected. */
enum class BittensorMevCarrier { ABSENT, INCLUDED, UNKNOWN }

class BittensorMevNotExecuted(val carrier: BittensorMevCarrier, val paidNetworkFee: BigInteger? = null) : Exception("The shielded order was not executed")

class BittensorMevNotReady(cause: Throwable? = null) : Exception("MEV protection is no longer available for this order", cause)

sealed class BittensorMevPaidFee(val amount: BigInteger) {

    class Known(amount: BigInteger) : BittensorMevPaidFee(amount)

    class Estimated(amount: BigInteger) : BittensorMevPaidFee(amount)
}

fun BittensorOperationResult.Failure.mevPaidFee(estimate: Fee): BittensorMevPaidFee? {
    val review = estimate as? BittensorMevReviewFee ?: return null
    if (!review.isProtected) return null
    val failure = cause as? BittensorMevNotExecuted
    val actual = when {
        cause is BittensorMevNotReady || failure?.carrier == BittensorMevCarrier.ABSENT -> BigInteger.ZERO
        failure != null -> failure.paidNetworkFee
        else -> outcome?.networkFee
    }
    if (actual != null) return BittensorMevPaidFee.Known(actual)
    val estimated = if (failure != null) requireNotNull(review.quote).carrierFee.amount else review.amount
    return BittensorMevPaidFee.Estimated(estimated)
}
