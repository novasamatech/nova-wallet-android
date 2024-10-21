package io.novafoundation.nova.feature_account_api.data.model

import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger

// TODO rename FeeBase -> Fee and use SubmissionFee everywhere Fee is currently used
typealias Fee = SubmissionFee

interface SubmissionFee : FeeBase {

    companion object

    /**
     * Information about origin that is supposed to send the transaction fee was calculated against
     */
    val submissionOrigin: SubmissionOrigin
}

/**
 * Fee that doesn't have a particular origin
 * For example, fees paid during cross chain transfers do not have a specific account that pays them
 */
interface FeeBase {

    val amount: BigInteger

    val asset: Chain.Asset
}

infix fun FeeBase.hasSameAssetAs(other: FeeBase): Boolean {
    return asset.fullId == other.asset.fullId
}

infix fun Fee.addPreservingOrigin(other: FeeBase): Fee {
    require(this hasSameAssetAs other) {
        "Cannot sum fees with different assets"
    }

    return addPlanks(other.amount)
}

infix fun Fee.addPlanks(planks: BigInteger): Fee {
    return SubstrateFee(amount + planks, submissionOrigin, asset)
}

fun Fee.replacePlanks(newPlanks: BigInteger): Fee {
    return  SubstrateFee(newPlanks, submissionOrigin, asset)
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

class SubstrateFeeBase(
    override val amount: BigInteger,
    override val asset: Chain.Asset
) : FeeBase

val Fee.requestedAccountPaysFees: Boolean
    get() = submissionOrigin.requestedOrigin.contentEquals(submissionOrigin.actualOrigin)

val Fee.amountByRequestedAccount: BigInteger
    get() = amount.asAmountByRequestedAccount

fun List<FeeBase>.totalAmount(chainAsset: Chain.Asset): BigInteger {
    return sumOf { it.getAmount(chainAsset) }
}

fun List<SubmissionFee>.totalAmount(chainAsset: Chain.Asset, origin: AccountId): BigInteger {
    return sumOf { it.getAmount(chainAsset, origin) }
}

fun List<FeeBase>.totalPlanksEnsuringAsset(requireAsset: Chain.Asset): BigInteger {
    return sumOf {
        require(it.asset.fullId == requireAsset.fullId) {
            "Atomic operation fee contains fee in different assets"
        }

        it.amount
    }
}

fun SubmissionFee.getAmount(chainAsset: Chain.Asset, origin: AccountId): BigInteger {
    return if (asset.fullId == chainAsset.fullId && submissionOrigin.actualOrigin.contentEquals(origin)) {
        amount
    } else {
        BigInteger.ZERO
    }
}

fun FeeBase.getAmount(expectedAsset: Chain.Asset): BigInteger {
    return if (expectedAsset.fullId == asset.fullId) amount else BigInteger.ZERO
}

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
