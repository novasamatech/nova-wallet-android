package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import java.math.BigInteger

enum class BittensorOperationKind { STAKE, UNSTAKE, CLAIM }

data class BittensorOutcomeTarget(
    val kind: BittensorOperationKind,
    val netuid: BittensorNetuid,
    val hotkeys: Set<AccountIdKey>,
    val serviceFeeBeneficiary: AccountIdKey? = null
)

/** All amounts are native integer units. Null means unavailable, never zero. */
data class BittensorOperationOutcome(
    val blockHash: String,
    val tao: BigInteger?,
    val alpha: BigInteger?,
    val serviceFee: BigInteger?,
    val networkFee: BigInteger?,
    val feePayer: AccountIdKey?,
    val alphaFee: BigInteger?,
    val netuid: BittensorNetuid,
    val coldkey: AccountIdKey,
    val alphaFeeNetuid: BittensorNetuid? = null
)

// A zero planned service fee means the reviewed builder contains no service transfer.
fun BittensorOperationOutcome.actualBuyPay(plannedServiceFee: BigInteger): BigInteger? {
    val executedTao = tao ?: return null
    val paidService = if (plannedServiceFee.isZero) BigInteger.ZERO else serviceFee ?: return null
    return executedTao + paidService
}

fun BittensorOperationOutcome.actualSellReceive(plannedServiceFee: BigInteger): BigInteger? {
    val executedTao = tao ?: return null
    val paidService = if (plannedServiceFee.isZero) BigInteger.ZERO else serviceFee ?: return null
    val payer = feePayer ?: return null
    // Alpha-paid fees were funded by the excluded fee sale, not by the executed native proceeds.
    val paidNetwork = if (payer == coldkey && alphaFee == null) networkFee ?: return null else BigInteger.ZERO
    return (executedTao - paidService - paidNetwork).takeIf { it.isNonNegative }
}

/** Root alpha is native-denominated; the fee sale is separate from the executed withdrawal. */
fun BittensorOperationOutcome.actualRootStakeMoved(): BigInteger? {
    if (!netuid.isRoot()) return null
    val executedTao = tao ?: return null
    val stakeFee = if (feePayer == coldkey && alphaFee != null) {
        val feeSubnet = alphaFeeNetuid ?: return null
        if (feeSubnet.isRoot()) alphaFee else BigInteger.ZERO
    } else {
        BigInteger.ZERO
    }
    return executedTao + stakeFee
}
