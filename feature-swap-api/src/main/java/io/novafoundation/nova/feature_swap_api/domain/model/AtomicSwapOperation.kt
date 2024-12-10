package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.totalAmount
import io.novafoundation.nova.feature_account_api.data.model.totalPlanksEnsuringAsset
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface AtomicSwapOperation {

    val estimatedSwapLimit: SwapLimit

    val assetIn: FullChainAssetId

    val assetOut: FullChainAssetId

    suspend fun constructDisplayData(): AtomicOperationDisplayData

    suspend fun estimateFee(): AtomicSwapOperationFee

    /**
     * Calculates how much of assetIn (of the current segment) is needed to buy given [extraOutAmount] of asset out (of the current segment)
     * Used to estimate how much extra amount of assetIn to add to the user input to accommodate future segment fees
     */
    suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance

    /**
     * Additional amount that max amount calculation should leave aside for the **first** operation in the swap
     * One example is Existential Deposit in case operation executes in "keep alive" manner
     */
    suspend fun additionalMaxAmountDeduction(): SwapMaxAdditionalAmountDeduction

    suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection>
}

class AtomicSwapOperationSubmissionArgs(
    val actualSwapLimit: SwapLimit,
)

class AtomicSwapOperationArgs(
    val estimatedSwapLimit: SwapLimit,
    val feePaymentCurrency: FeePaymentCurrency,
)

fun AtomicSwapOperationFee.amountToLeaveOnOriginToPayTxFees(): Balance {
    val submissionAsset = submissionFee.asset
    return submissionFee.amount + postSubmissionFees.paidByAccount.totalAmount(submissionAsset, submissionFee.submissionOrigin.executingAccount)
}

fun AtomicSwapOperationFee.totalFeeEnsuringSubmissionAsset(): Balance {
    val postSubmissionFeesByAccount = postSubmissionFees.paidByAccount.totalPlanksEnsuringAsset(submissionFee.asset)
    val postSubmissionFeesFromHolding = postSubmissionFees.paidByAccount.totalPlanksEnsuringAsset(submissionFee.asset)

    return submissionFee.amount + postSubmissionFeesByAccount + postSubmissionFeesFromHolding
}

/**
 * Collects all [FeeBase] instances from fee components
 */
fun AtomicSwapOperationFee.allBasicFees(): List<FeeBase> {
    return buildList {
        add(submissionFee)
        postSubmissionFees.paidByAccount.onEach(::add)
        postSubmissionFees.paidFromAmount.onEach(::add)
    }
}

fun AtomicSwapOperationFee.allFeeAssets(): List<Chain.Asset> {
    return allBasicFees()
        .map { it.asset }
        .distinctBy { it.fullId }
}

class SwapExecutionCorrection(
    val actualReceivedAmount: Balance
)
