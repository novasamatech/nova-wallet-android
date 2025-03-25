package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.common.utils.amountFromPlanks
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.maxAction.MaxAvailableDeduction
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigDecimal
import java.math.BigInteger

// TODO rename FeeBase -> Fee and use SubmissionFee everywhere Fee is currently used
typealias Fee = SubmissionFee

interface SubmissionFee : FeeBase, MaxAvailableDeduction {

    companion object

    /**
     * Information about origin that is supposed to send the transaction fee was calculated against
     */
    val submissionOrigin: SubmissionOrigin

    /**
     * Submission fee deducts fee amount from max balance only when executing account pays fees
     * When signing account is different from executing one, executing account balance remains unaffected by submission fee payment
     */
    override fun maxAmountDeductionFor(amountAsset: Chain.Asset): BigInteger {
        return getAmountByExecutingAccount(amountAsset)
    }
}

val SubmissionFee.submissionFeesPayer: AccountId
    get() = submissionOrigin.signingAccount

/**
 * Fee that doesn't have a particular origin
 * For example, fees paid during cross chain transfers do not have a specific account that pays them
 */
interface FeeBase {

    val amount: BigInteger

    val asset: Chain.Asset
}

val FeeBase.decimalAmount: BigDecimal
    get() = amount.amountFromPlanks(asset.precision)

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

class SubstrateFeeBase(
    override val amount: BigInteger,
    override val asset: Chain.Asset
) : FeeBase

val Fee.amountByExecutingAccount: BigInteger
    get() = getAmount(asset, submissionOrigin.executingAccount)

val Fee.decimalAmountByExecutingAccount: BigDecimal
    get() = amountByExecutingAccount.amountFromPlanks(asset.precision)

fun FeeBase.addPlanks(extraPlanks: BigInteger): FeeBase {
    return SubstrateFeeBase(amount + extraPlanks, asset)
}

fun List<FeeBase>.totalAmount(chainAsset: Chain.Asset): BigInteger {
    return sumOf { it.getAmount(chainAsset) }
}

fun List<SubmissionFee>.totalAmount(chainAsset: Chain.Asset, origin: AccountId): BigInteger {
    return sumOf { it.getAmount(chainAsset, origin) }
}

fun List<FeeBase>.totalPlanksEnsuringAsset(requireAsset: Chain.Asset): BigInteger {
    return sumOf {
        require(it.asset.fullId == requireAsset.fullId) {
            "Fees contain fee in different assets: ${it.asset.fullId}"
        }

        it.amount
    }
}

fun SubmissionFee.getAmount(chainAsset: Chain.Asset, origin: AccountId): BigInteger {
    return if (asset.fullId == chainAsset.fullId && submissionFeesPayer.contentEquals(origin)) {
        amount
    } else {
        BigInteger.ZERO
    }
}

fun SubmissionFee.getAmountByExecutingAccount(chainAsset: Chain.Asset): BigInteger {
    return getAmount(chainAsset, submissionOrigin.executingAccount)
}

fun FeeBase.getAmount(expectedAsset: Chain.Asset): BigInteger {
    return if (expectedAsset.fullId == asset.fullId) amount else BigInteger.ZERO
}
