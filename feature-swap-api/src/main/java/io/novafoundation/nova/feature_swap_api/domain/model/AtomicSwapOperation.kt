package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_account_api.data.model.totalAmount
import io.novafoundation.nova.feature_account_api.data.model.totalPlanksEnsuringAsset
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee.SwapSegment.SegmentNetFlow
import io.novafoundation.nova.feature_swap_api.domain.model.fee.AtomicSwapOperationFee
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

interface AtomicSwapOperation {

    val estimatedSwapLimit: SwapLimit

    val assetIn: FullChainAssetId

    val assetOut: FullChainAssetId

    /**
     * Whether this operation levies a Nova service fee on its output. The commission is charged by
     * the **last** operation in the route for which this is true (see [AtomicSwapOperationSubmissionArgs.isServiceCommissionOperation]).
     */
    val chargesServiceCommission: Boolean
        get() = false

    /**
     * Builds the UI representation of this segment. The operation only supplies its assets and shape — the
     * amounts come from [netFlow], which is resolved once for the whole route by [buildSwapSegments] and is
     * already net of the Nova service commission.
     */
    suspend fun constructDisplayData(netFlow: SegmentNetFlow): AtomicOperationDisplayData

    /**
     * @param isServiceCommissionOperation true only for the last operation in the route that charges the
     * Nova service commission — mirrors [AtomicSwapOperationSubmissionArgs.isServiceCommissionOperation],
     * so the estimate includes exactly the calls that will actually be submitted.
     */
    suspend fun estimateFee(isServiceCommissionOperation: Boolean): AtomicSwapOperationFee

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

    suspend fun execute(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection>

    suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapSubmissionResult>
}

class AtomicSwapOperationSubmissionArgs(
    val actualSwapLimit: SwapLimit,
    val isServiceCommissionOperation: Boolean,
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

class SwapSubmissionResult(
    val submissionHierarchy: SubmissionHierarchy
)
