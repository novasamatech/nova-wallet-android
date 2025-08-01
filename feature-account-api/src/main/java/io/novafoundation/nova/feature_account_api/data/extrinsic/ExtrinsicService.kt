package io.novafoundation.nova.feature_account_api.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentCurrency
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

typealias FormExtrinsicWithOrigin = suspend ExtrinsicBuilder.(context: ExtrinsicBuildingContext) -> Unit
typealias FormMultiExtrinsicWithOrigin = suspend CallBuilder.(context: ExtrinsicBuildingContext) -> Unit

class ExtrinsicSubmission(
    val hash: String,
    val submissionOrigin: SubmissionOrigin,
    val callExecutionType: CallExecutionType,
    val submissionHierarchy: SubmissionHierarchy
)

class ExtrinsicBuildingContext(
    val submissionOrigin: SubmissionOrigin,
    val signer: NovaSigner,
    val chain: Chain
)

private val DEFAULT_BATCH_MODE = BatchMode.BATCH_ALL

interface ExtrinsicService {

    interface Factory {

        fun create(feeConfig: FeePaymentConfig): ExtrinsicService
    }

    class SubmissionOptions(
        val feePaymentCurrency: FeePaymentCurrency = FeePaymentCurrency.Native,
        val batchMode: BatchMode = DEFAULT_BATCH_MODE,
    )

    class FeePaymentConfig(
        val coroutineScope: CoroutineScope,
        /**
         * Specify to use it instead of default [FeePaymentProviderRegistry] to perform fee computations
         */
        val customFeePaymentRegistry: FeePaymentProviderRegistry? = null,
    )

    suspend fun submitExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<ExtrinsicSubmission>

    suspend fun submitAndWatchExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>>

    suspend fun submitExtrinsicAndAwaitExecution(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<ExtrinsicExecutionResult>

    suspend fun submitMultiExtrinsicAwaitingInclusion(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): RetriableMultiResult<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>>

    suspend fun paymentInfo(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormExtrinsicWithOrigin
    ): FeeResponse

    suspend fun estimateFee(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormExtrinsicWithOrigin
    ): Fee

    suspend fun estimateMultiFee(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions = SubmissionOptions(),
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): Fee

    suspend fun estimateFee(
        chain: Chain,
        extrinsic: String,
        usedSigner: NovaSigner,
    ): Fee
}
