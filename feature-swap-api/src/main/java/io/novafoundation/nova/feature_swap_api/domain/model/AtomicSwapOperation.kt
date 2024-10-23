package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.model.totalAmount
import io.novafoundation.nova.feature_account_api.data.model.totalPlanksEnsuringAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

interface AtomicSwapOperation {

    val estimatedSwapLimit: SwapLimit

    suspend fun estimateFee(): AtomicSwapOperationFee

    suspend fun requiredAmountInToGetAmountOut(extraOutAmount: Balance): Balance

    /**
     * Additional amount that max amount calculation should leave aside for the **first** operation in the swap
     * One example is Existential Deposit in case operation executes in "keep alive" manner
     */
    suspend fun additionalMaxAmountDeduction(): Balance

    // TODO this is a temporarily function until we developer Operation Manager
    suspend fun inProgressLabel(): String

    suspend fun submit(args: AtomicSwapOperationSubmissionArgs): Result<SwapExecutionCorrection>
}

class AtomicSwapOperationSubmissionArgs(
    val actualSwapLimit: SwapLimit,
)

class AtomicSwapOperationArgs(
    val estimatedSwapLimit: SwapLimit,
    val feePaymentCurrency: FeePaymentCurrency,
)

class AtomicSwapOperationFee(
    /**
     * Fee that is paid when submitting transaction
     */
    val submissionFee: SubmissionFeeWithLabel,

    val postSubmissionFees: PostSubmissionFees = PostSubmissionFees(),
) {

    class PostSubmissionFees(
        /**
         * Post-submission fees paid by (some) origin account.
         * This is typed as `SubmissionFee` as those fee might still use different accounts (e.g. delivery fees are always paid from requested account)
         */
        val paidByAccount: List<SubmissionFeeWithLabel> = emptyList(),

        /**
         * Post-submission fees paid from swapping amount directly. Its payment is isolated and does not involve any withdrawals from accounts
         */
        val paidFromAmount: List<FeeWithLabel> = emptyList()
    )
}

fun AtomicSwapOperationFee.amountToLeaveOnOriginToPayTxFees(): Balance {
    val submissionAsset = submissionFee.asset
    return submissionFee.amount + postSubmissionFees.paidByAccount.totalAmount(submissionAsset, submissionFee.submissionOrigin.executingAccount)
}

fun AtomicSwapOperationFee.totalFeeEnsuringSubmissionAsset(): Balance {
    val postSubmissionFeesByAccount = postSubmissionFees.paidByAccount.totalPlanksEnsuringAsset(submissionFee.asset)
    val postSubmissionFeesFromHolding = postSubmissionFees.paidByAccount.totalPlanksEnsuringAsset(submissionFee.asset)

    return submissionFee.amount + postSubmissionFeesByAccount + postSubmissionFeesFromHolding
}

class SwapExecutionCorrection(
    val actualReceivedAmount: Balance
)
