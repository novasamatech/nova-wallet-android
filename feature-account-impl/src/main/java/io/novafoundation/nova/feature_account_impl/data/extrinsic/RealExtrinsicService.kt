package io.novafoundation.nova.feature_account_impl.data.extrinsic

import android.util.Log
import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.common.utils.multiResult.runMultiCatching
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.takeWhileInclusive
import io.novafoundation.nova.common.utils.tip
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService.SubmissionOptions
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsic
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.awaitInBlock
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicDispatch
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.common.data.network.runtime.binding.bindDispatchError
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.feature_account_api.data.fee.FeePaymentProviderRegistry
import io.novafoundation.nova.feature_account_api.data.fee.toChainAsset
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireMetaAccountFor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import io.novafoundation.nova.runtime.extrinsic.signer.FeeSigner
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
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import java.math.BigInteger

class RealExtrinsicService(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicBuilderFactory: ExtrinsicBuilderFactory,
    private val signerProvider: SignerProvider,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val feePaymentProviderRegistry: FeePaymentProviderRegistry,
    private val eventsRepository: EventsRepository,
    private val coroutineScope: CoroutineScope? // TODO: Make it non-nullable
) : ExtrinsicService {

    override suspend fun submitExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<ExtrinsicSubmission> = runCatching {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        val (extrinsic, submissionOrigin) = buildExtrinsic(chain, metaAccount, formExtrinsic, submissionOptions)
        val hash = rpcCalls.submitExtrinsic(chain.id, extrinsic)

        ExtrinsicSubmission(hash, submissionOrigin)
    }

    override suspend fun submitMultiExtrinsicAwaitingInclusion(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): RetriableMultiResult<ExtrinsicStatus.InBlock> {
        return runMultiCatching(
            intermediateListLoading = { constructSplitExtrinsicsForSubmission(chain, origin, submissionOptions, formExtrinsic) },
            listProcessing = { extrinsic ->
                rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
                    .filterIsInstance<ExtrinsicStatus.InBlock>()
                    .first()
            }
        )
    }

    // TODO: The flow in Result may produce an exception that will be not handled since Result can't catch an exception inside a flow
    // For now it's handling in awaitInBlock() extension
    override suspend fun submitAndWatchExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<Flow<ExtrinsicStatus>> = runCatching {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        val (extrinsic) = buildExtrinsic(chain, metaAccount, formExtrinsic, submissionOptions)

        rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
            .takeWhileInclusive { !it.terminal }
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
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit
    ): FeeResponse {
        val extrinsic = extrinsicBuilderFactory.createForFee(getFeeSigner(chain, origin), chain)
            .also { it.formExtrinsic() }
            .buildExtrinsic(submissionOptions.batchMode)

        return rpcCalls.getExtrinsicFee(chain, extrinsic)
    }

    override suspend fun estimateFee(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit
    ): Fee {
        val signer = getFeeSigner(chain, origin)
        val extrinsicBuilder = extrinsicBuilderFactory.createForFee(signer, chain)
        extrinsicBuilder.formExtrinsic()

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)

        feePayment.modifyExtrinsic(extrinsicBuilder)
        val extrinsic = extrinsicBuilder.buildExtrinsic(submissionOptions.batchMode)

        val nativeFee = estimateNativeFee(chain, extrinsic, signer.submissionOrigin(chain))
        return feePayment.convertNativeFee(nativeFee)
    }

    override suspend fun estimateFee(
        chain: Chain,
        extrinsic: String,
        usedSigner: FeeSigner,
    ): Fee {
        val runtime = chainRegistry.getRuntime(chain.id)
        val sendableExtrinsic = SendableExtrinsic(runtime, Extrinsic.fromHex(runtime, extrinsic))

        val nativeFee = estimateNativeFee(chain, sendableExtrinsic, usedSigner.submissionOrigin(chain))

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.detectFeePaymentFromExtrinsic(sendableExtrinsic)

        return feePayment.convertNativeFee(nativeFee)
    }

    override suspend fun zeroFee(chain: Chain, origin: TransactionOrigin, submissionOptions: SubmissionOptions): Fee {
        val signer = getFeeSigner(chain, origin)
        return getZeroFee(chain, signer, submissionOptions)
    }

    override suspend fun estimateMultiFee(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormMultiExtrinsic
    ): Fee {
        val feeSigner = getFeeSigner(chain, origin)
        val feeExtrinsicBuilderSequence = extrinsicBuilderFactory.createMultiForFee(feeSigner, chain)

        val extrinsics = constructSplitExtrinsics(
            chain = chain,
            origin = origin,
            formExtrinsic = formExtrinsic,
            extrinsicBuilderSequence = feeExtrinsicBuilderSequence,
            alreadyComputedFeeSigner = feeSigner,
            submissionOptions = submissionOptions
        )

        if (extrinsics.isEmpty()) return getZeroFee(chain, feeSigner, submissionOptions)

        val fees = extrinsics.mapAsync { estimateNativeFee(chain, it, feeSigner.submissionOrigin(chain)) }
        val totalFeeAmount = fees.sumOf { it.amount }

        val totalNativeFee = SubstrateFee(
            totalFeeAmount,
            feeSigner.submissionOrigin(chain),
            submissionOptions.feePaymentCurrency.toChainAsset(chain)
        )

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)

        return feePayment.convertNativeFee(totalNativeFee)
    }

    private suspend fun determineExtrinsicOutcome(
        inBlock: ExtrinsicStatus.InBlock,
        chain: Chain
    ): ExtrinsicExecutionResult {
        val outcome = runCatching {
            val extrinsicWithEvents = eventsRepository.getExtrinsicWithEvents(chain.id, inBlock.extrinsicHash, inBlock.blockHash)
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
            extrinsicHash = inBlock.extrinsicHash,
            blockHash = inBlock.blockHash,
            outcome = outcome
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

    private suspend fun constructSplitExtrinsicsForSubmission(
        chain: Chain,
        origin: TransactionOrigin,
        submissionOptions: SubmissionOptions,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): List<SendableExtrinsic> {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        val signer = signerProvider.rootSignerFor(metaAccount)

        val requestedOrigin = metaAccount.requireAccountIdIn(chain)
        val actualOrigin = signer.signerAccountId(chain)
        val submissionOrigin = SubmissionOrigin(executingAccount = requestedOrigin, signingAccount = actualOrigin)

        val extrinsicBuilderSequence = extrinsicBuilderFactory.createMulti(chain, signer, requestedOrigin)

        val formExtrinsicWithOrigin: FormMultiExtrinsic = { formExtrinsic(submissionOrigin) }

        return constructSplitExtrinsics(chain, origin, formExtrinsicWithOrigin, extrinsicBuilderSequence, submissionOptions)
    }

    private suspend fun constructSplitExtrinsics(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsic,
        extrinsicBuilderSequence: Sequence<ExtrinsicBuilder>,
        submissionOptions: SubmissionOptions,
        alreadyComputedFeeSigner: FeeSigner? = null,
    ): List<SendableExtrinsic> = coroutineScope {
        val feeSigner = alreadyComputedFeeSigner ?: getFeeSigner(chain, origin)
        val runtime = chainRegistry.getRuntime(chain.id)

        val callBuilder = SimpleCallBuilder(runtime).apply { formExtrinsic() }
        val splitCalls = extrinsicSplitter.split(feeSigner, callBuilder, chain)

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)

        val extrinsicBuilderIterator = extrinsicBuilderSequence.iterator()

        splitCalls.map { batch ->
            val extrinsicBuilder = extrinsicBuilderIterator.next()

            batch.forEach(extrinsicBuilder::call)

            feePayment.modifyExtrinsic(extrinsicBuilder)

            extrinsicBuilder.buildExtrinsic(submissionOptions.batchMode)
        }
    }

    private suspend fun buildExtrinsic(
        chain: Chain,
        metaAccount: MetaAccount,
        formExtrinsic: FormExtrinsicWithOrigin,
        submissionOptions: SubmissionOptions,
    ): Submission {
        val signer = signerProvider.rootSignerFor(metaAccount)

        val requestedOrigin = metaAccount.requireAccountIdIn(chain)
        val actualOrigin = signer.signerAccountId(chain)

        val submissionOrigin = SubmissionOrigin(executingAccount = requestedOrigin, signingAccount = actualOrigin)

        val extrinsicBuilder = extrinsicBuilderFactory.create(chain, signer, requestedOrigin)
        extrinsicBuilder.formExtrinsic(submissionOrigin)

        val feePaymentProvider = feePaymentProviderRegistry.providerFor(chain.id)
        val feePayment = feePaymentProvider.feePaymentFor(submissionOptions.feePaymentCurrency, coroutineScope)

        feePayment.modifyExtrinsic(extrinsicBuilder)

        val extrinsic = extrinsicBuilder.buildExtrinsic(submissionOptions.batchMode)

        return Submission(extrinsic, submissionOrigin)
    }

    private suspend fun getFeeSigner(chain: Chain, origin: TransactionOrigin): FeeSigner {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        return signerProvider.feeSigner(metaAccount, chain)
    }

    private data class Submission(val extrinsic: SendableExtrinsic, val submissionOrigin: SubmissionOrigin)

    private suspend fun FeeSigner.submissionOrigin(chain: Chain): SubmissionOrigin {
        return SubmissionOrigin(requestedFeeSignerId(chain = chain), actualFeeSignerId(chain))
    }

    private suspend fun getZeroFee(chain: Chain, signer: FeeSigner, submissionOptions: SubmissionOptions): Fee {
        return SubstrateFee(
            BigInteger.ZERO,
            signer.submissionOrigin(chain),
            submissionOptions.feePaymentCurrency.toChainAsset(chain)
        )
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
