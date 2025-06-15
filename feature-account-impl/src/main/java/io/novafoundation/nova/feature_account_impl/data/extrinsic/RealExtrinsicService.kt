package io.novafoundation.nova.feature_account_impl.data.extrinsic

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchError
import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.common.utils.multiResult.runMultiCatching
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.common.utils.takeWhileInclusive
import io.novafoundation.nova.common.utils.tip
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicBuildingContext
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService.SubmissionOptions
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.watch.ExtrinsicWatchResult
import io.novafoundation.nova.feature_account_api.data.fee.FeePayment
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.toChainAsset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.data.signer.SigningMode
import io.novafoundation.nova.feature_account_api.data.signer.setSignerData
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireMetaAccountFor
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_account_impl.data.signer.signingContext.withSequenceSigning
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.EventsRepository
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.ExtrinsicWithEvents
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.findExtrinsicFailureOrThrow
import io.novafoundation.nova.runtime.multiNetwork.runtime.repository.isSuccess
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Extrinsic
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericEvent
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RealExtrinsicService(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicBuilderFactory: ExtrinsicBuilderFactory,
    private val signerProvider: SignerProvider,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val eventsRepository: EventsRepository,
    private val signingContextFactory: SigningContext.Factory,
    private val coroutineScope: CoroutineScope? // TODO: Make it non-nullable
) : ExtrinsicService {

    override suspend fun submitExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<ExtrinsicSubmission> = runCatching {
        val (extrinsic, submissionOrigin, _, callExecutionType, signingHierarchy) = buildSubmissionExtrinsic(chain, origin, formExtrinsic, submissionOptions)
        val hash = rpcCalls.submitExtrinsic(chain.id, extrinsic)

        ExtrinsicSubmission(hash, submissionOrigin, callExecutionType, signingHierarchy)
    }

    override suspend fun submitMultiExtrinsicAwaitingInclusion(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): RetriableMultiResult<ExtrinsicWatchResult<ExtrinsicStatus.InBlock>> {
        return runMultiCatching(
            intermediateListLoading = {
                val submission = constructSplitExtrinsics(chain, origin, formExtrinsic, submissionOptions, SigningMode.SUBMISSION)

                submission.extrinsics.map { it to submission }
            },
            listProcessing = { (extrinsic, submission) ->
                rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
                    .filterIsInstance<ExtrinsicStatus.InBlock>()
                    .map { ExtrinsicWatchResult(it, submission.submissionHierarchy) }
                    .first()
            }
        )
    }

    // TODO: The flow in Result may produce an exception that will be not handled since Result can't catch an exception inside a flow
    // For now it's handled in awaitInBlock() extension
    override suspend fun submitAndWatchExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<Flow<ExtrinsicWatchResult<ExtrinsicStatus>>> = runCatching {
        val singleSubmission = buildSubmissionExtrinsic(chain, origin, formExtrinsic, submissionOptions)

        rpcCalls.submitAndWatchExtrinsic(chain.id, singleSubmission.extrinsic)
            .map { ExtrinsicWatchResult(it, singleSubmission.submissionHierarchy) }
            .takeWhileInclusive { !it.status.terminal }
    }

    override suspend fun submitExtrinsicAndAwaitExecution(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<ExtrinsicExecutionResult> {
        return submitAndWatchExtrinsic(chain, origin, submissionOptions, formExtrinsic)
            .awaitInBlock()
            .map { determineExtrinsicOutcome(it, chain) }
    }

    override suspend fun paymentInfo(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): FeeResponse {
        val (extrinsic) = buildFeeExtrinsic(chain, origin, formExtrinsic, submissionOptions)
        return rpcCalls.getExtrinsicFee(chain, extrinsic)
    }

    override suspend fun estimateFee(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Fee {
        val (extrinsic, submissionOrigin, feePayment) = buildFeeExtrinsic(chain, origin, formExtrinsic, submissionOptions)
        val nativeFee = estimateNativeFee(chain, extrinsic, submissionOrigin)
        return feePayment.convertNativeFee(nativeFee)
    }

    override suspend fun estimateFee(
        chain: Chain,
        extrinsic: String,
        usedSigner: NovaSigner,
    ): Fee {
        val runtime = chainRegistry.getRuntime(chain.id)
        val sendableExtrinsic = SendableExtrinsic(runtime, Extrinsic.fromHex(runtime, extrinsic))
        val submissionOrigin = usedSigner.submissionOrigin(chain)

        val nativeFee = estimateNativeFee(chain, sendableExtrinsic, submissionOrigin)

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.detectFeePaymentFromExtrinsic(sendableExtrinsic)

        return feePayment.convertNativeFee(nativeFee)
    }

    override suspend fun estimateMultiFee(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): Fee {
        val (extrinsics, submissionOrigin) = constructSplitExtrinsics(chain, origin, formExtrinsic, submissionOptions, SigningMode.FEE)
        require(extrinsics.isNotEmpty()) { "Empty extrinsics list" }

        val fees = extrinsics.mapAsync { estimateNativeFee(chain, it, submissionOrigin) }
        val totalFeeAmount = fees.sumOf { it.amount }

        val totalNativeFee = SubstrateFee(
            amount = totalFeeAmount,
            submissionOrigin = submissionOrigin,
            asset = submissionOptions.feePaymentCurrency.toChainAsset(chain)
        )

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)

        return feePayment.convertNativeFee(totalNativeFee)
    }

    private suspend fun determineExtrinsicOutcome(
        watchResult: ExtrinsicWatchResult<ExtrinsicStatus.InBlock>,
        chain: Chain
    ): ExtrinsicExecutionResult {
        val status = watchResult.status

        val outcome = runCatching {
            val extrinsicWithEvents = eventsRepository.getExtrinsicWithEvents(chain.id, status.extrinsicHash, status.blockHash)
            val runtime = chainRegistry.getRuntime(chain.id)

            requireNotNull(extrinsicWithEvents) {
                "No extrinsic included into expected block"
            }

            extrinsicWithEvents.determineOutcome(runtime)
        }.getOrElse {
            Log.w(LOG_TAG, "Failed to determine extrinsic outcome", it)

            ExtrinsicDispatch.Unknown
        }

        return ExtrinsicExecutionResult(
            extrinsicHash = status.extrinsicHash,
            blockHash = status.blockHash,
            outcome = outcome,
            submissionHierarchy = watchResult.submissionHierarchy
        )
    }

    private fun ExtrinsicWithEvents.determineOutcome(runtimeSnapshot: RuntimeSnapshot): ExtrinsicDispatch {
        return if (isSuccess()) {
            ExtrinsicDispatch.Ok(events)
        } else {
            val errorEvent = events.findExtrinsicFailureOrThrow()
            val dispatchError = parseErrorEvent(errorEvent, runtimeSnapshot)

            ExtrinsicDispatch.Failed(dispatchError)
        }
    }

    private fun parseErrorEvent(errorEvent: GenericEvent.Instance, runtimeSnapshot: RuntimeSnapshot): DispatchError {
        val dispatchError = errorEvent.arguments.first()

        return runtimeSnapshot.provideContext { bindDispatchError(dispatchError) }
    }

    private suspend fun constructSplitExtrinsics(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsicWithOrigin,
        submissionOptions: SubmissionOptions,
        signingMode: SigningMode
    ): MultiSubmission {
        val signer = getSigner(chain, origin)

        val extrinsicBuilderSequence = extrinsicBuilderFactory.createMulti(
            chain = chain,
            options = submissionOptions.toBuilderFactoryOptions()
        )

        val runtime = chainRegistry.getRuntime(chain.id)

        val submissionOrigin = signer.submissionOrigin(chain)
        val buildingContext = ExtrinsicBuildingContext(submissionOrigin, signer, chain)

        val callBuilder = SimpleCallBuilder(runtime).apply { formExtrinsic(buildingContext) }
        val splitCalls = extrinsicSplitter.split(signer, callBuilder, chain)

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)

        val extrinsicBuilderIterator = extrinsicBuilderSequence.iterator()

        // Setup signing
        val signingContext = signingContextFactory.default(chain).withSequenceSigning()
        val extrinsics = splitCalls.map { batch ->
            // Create empty builder
            val extrinsicBuilder = extrinsicBuilderIterator.next()

            // Add upstream calls
            batch.forEach(extrinsicBuilder::call)

            // Setup fees
            feePayment.modifyExtrinsic(extrinsicBuilder)

            // Setup signing
            with(extrinsicBuilder) {
                signer.setSignerData(signingContext, signingMode)
            }

            // Build extrinsic
            extrinsicBuilder.buildExtrinsic().also {
                signingContext.incrementNonceOffset()
            }
        }

        val signingHierarchy = signer.getSigningHierarchy()

        return MultiSubmission(extrinsics, submissionOrigin, feePayment, signingHierarchy)
    }

    private suspend fun buildSubmissionExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormExtrinsicWithOrigin,
        submissionOptions: SubmissionOptions,
    ): SingleSubmission {
        return buildExtrinsic(chain, origin, formExtrinsic, submissionOptions, SigningMode.SUBMISSION)
    }

    private suspend fun buildFeeExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormExtrinsicWithOrigin,
        submissionOptions: SubmissionOptions,
    ): SingleSubmission {
        return buildExtrinsic(chain, origin, formExtrinsic, submissionOptions, SigningMode.FEE)
    }

    private suspend fun buildExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormExtrinsicWithOrigin,
        submissionOptions: SubmissionOptions,
        signingMode: SigningMode
    ): SingleSubmission {
        val signer = getSigner(chain, origin)

        val submissionOrigin = signer.submissionOrigin(chain)

        // Create empty builder
        val extrinsicBuilder = extrinsicBuilderFactory.create(
            chain = chain,
            options = submissionOptions.toBuilderFactoryOptions()
        )

        // Add upstream calls
        val buildingContext = ExtrinsicBuildingContext(submissionOrigin, signer, chain)
        extrinsicBuilder.formExtrinsic(buildingContext)

        // Setup fees
        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)
        feePayment.modifyExtrinsic(extrinsicBuilder)

        // Setup signing
        val signingContext = signingContextFactory.default(chain)
        with(extrinsicBuilder) {
            signer.setSignerData(signingContext, signingMode)
        }

        // Build extrinsic
        val extrinsic = extrinsicBuilder.buildExtrinsic()

        val signingHierarchy = signer.getSigningHierarchy()

        return SingleSubmission(extrinsic, submissionOrigin, feePayment, signer.callExecutionType(), signingHierarchy)
    }

    private fun SubmissionOptions.toBuilderFactoryOptions(): ExtrinsicBuilderFactory.Options {
        return ExtrinsicBuilderFactory.Options(batchMode)
    }

    private suspend fun getSigner(chain: Chain, origin: TransactionOrigin): NovaSigner {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        return signerProvider.rootSignerFor(metaAccount)
    }

    private data class SingleSubmission(
        val extrinsic: SendableExtrinsic,
        val submissionOrigin: SubmissionOrigin,
        val feePayment: FeePayment,
        val callExecutionType: CallExecutionType,
        val submissionHierarchy: SubmissionHierarchy,
    )

    private data class MultiSubmission(
        val extrinsics: List<SendableExtrinsic>,
        val submissionOrigin: SubmissionOrigin,
        val feePayment: FeePayment,
        val submissionHierarchy: SubmissionHierarchy
    )

    private suspend fun NovaSigner.submissionOrigin(chain: Chain): SubmissionOrigin {
        val executingAccount = metaAccount.requireAccountIdIn(chain)
        val signingAccount = submissionSignerAccountId(chain)
        return SubmissionOrigin(executingAccount, signingAccount)
    }

    private suspend fun estimateNativeFee(
        chain: Chain,
        sendableExtrinsic: SendableExtrinsic,
        submissionOrigin: SubmissionOrigin
    ): Fee {
        val baseFee = rpcCalls.getExtrinsicFee(chain, sendableExtrinsic).partialFee
        val tip = sendableExtrinsic.extrinsic.tip().orZero()

        return SubstrateFee(
            amount = tip + baseFee,
            submissionOrigin = submissionOrigin,
            chain.commissionAsset
        )
    }
}
