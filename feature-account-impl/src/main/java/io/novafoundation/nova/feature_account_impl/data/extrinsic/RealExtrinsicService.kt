package io.novafoundation.nova.feature_account_impl.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.common.utils.multiResult.runMultiCatching
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sum
import io.novafoundation.nova.common.utils.takeWhileInclusive
import io.novafoundation.nova.common.utils.tip
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsic
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.requireMetaAccountFor
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

class RealExtrinsicService(
    private val rpcCalls: RpcCalls,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicBuilderFactory: ExtrinsicBuilderFactory,
    private val signerProvider: SignerProvider,
    private val extrinsicSplitter: ExtrinsicSplitter,
) : ExtrinsicService {

    override suspend fun submitExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<ExtrinsicSubmission> = runCatching {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        val (extrinsic, submissionOrigin) = buildExtrinsic(chain, metaAccount, formExtrinsic)
        val hash = rpcCalls.submitExtrinsic(chain.id, extrinsic)

        ExtrinsicSubmission(hash, submissionOrigin)
    }

    override suspend fun submitMultiExtrinsicAwaitingInclusion(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): RetriableMultiResult<ExtrinsicStatus.InBlock> {
        return runMultiCatching(
            intermediateListLoading = { constructSplitExtrinsicsForSubmission(chain, origin, formExtrinsic) },
            listProcessing = { extrinsic ->
                rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
                    .filterIsInstance<ExtrinsicStatus.InBlock>()
                    .first()
            }
        )
    }

    override suspend fun submitAndWatchExtrinsic(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormExtrinsicWithOrigin
    ): Result<Flow<ExtrinsicStatus>> = runCatching {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        val (extrinsic) = buildExtrinsic(chain, metaAccount, formExtrinsic)

        rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
            .takeWhileInclusive { !it.terminal }
    }

    override suspend fun paymentInfo(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): FeeResponse {
        val extrinsic = extrinsicBuilderFactory.createForFee(getFeeSigner(chain, origin), chain)
            .also { it.formExtrinsic() }
            .build()

        return rpcCalls.getExtrinsicFee(chain, extrinsic)
    }

    override suspend fun estimateFee(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit
    ): Fee {
        val extrinsicBuilder = extrinsicBuilderFactory.createForFee(getFeeSigner(chain, origin), chain)
        extrinsicBuilder.formExtrinsic()
        val extrinsic = extrinsicBuilder.build()

        return estimateFee(chain, extrinsic)
    }

    override suspend fun estimateFee(chain: Chain, extrinsic: String): Fee {
        val chainId = chain.id
        val baseFee = rpcCalls.getExtrinsicFee(chain, extrinsic).partialFee

        val runtime = chainRegistry.getRuntime(chainId)

        val decodedExtrinsic = Extrinsic.fromHex(runtime, extrinsic)

        val tip = decodedExtrinsic.tip().orZero()

        return InlineFee(tip + baseFee)
    }

    override suspend fun estimateMultiFee(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsic
    ): Fee {
        val feeExtrinsicBuilderSequence = extrinsicBuilderFactory.createMultiForFee(getFeeSigner(chain, origin), chain)

        val extrinsics = constructSplitExtrinsics(chain, origin, formExtrinsic, feeExtrinsicBuilderSequence)

        val separateFees = extrinsics.map { estimateFee(chain, it).amount }
        val totalFee = separateFees.sum()

        return InlineFee(totalFee)
    }

    private suspend fun constructSplitExtrinsicsForSubmission(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsicWithOrigin,
    ): List<String> {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        val signer = signerProvider.rootSignerFor(metaAccount)

        val requestedOrigin = metaAccount.requireAccountIdIn(chain)
        val actualOrigin = signer.signerAccountId(chain)
        val submissionOrigin = SubmissionOrigin(requestedOrigin = requestedOrigin, actualOrigin = actualOrigin)

        val extrinsicBuilderSequence = extrinsicBuilderFactory.createMulti(chain, signer, requestedOrigin)

        val formExtrinsicWithOrigin: FormMultiExtrinsic = { formExtrinsic(submissionOrigin) }

        return constructSplitExtrinsics(chain, origin, formExtrinsicWithOrigin, extrinsicBuilderSequence)
    }

    private suspend fun constructSplitExtrinsics(
        chain: Chain,
        origin: TransactionOrigin,
        formExtrinsic: FormMultiExtrinsic,
        extrinsicBuilderSequence: Sequence<ExtrinsicBuilder>,
    ): List<String> = coroutineScope {
        val runtime = chainRegistry.getRuntime(chain.id)

        val callBuilder = SimpleCallBuilder(runtime).apply { formExtrinsic() }
        val splitCalls = extrinsicSplitter.split(getFeeSigner(chain, origin), callBuilder, chain)

        val extrinsicBuilderIterator = extrinsicBuilderSequence.iterator()

        val extrinsicsToSubmit = splitCalls.map { batch ->
            val extrinsicBuilder = extrinsicBuilderIterator.next()

            batch.forEach(extrinsicBuilder::call)

            extrinsicBuilder.build()
        }

        extrinsicsToSubmit
    }

    private suspend fun buildExtrinsic(
        chain: Chain,
        metaAccount: MetaAccount,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): SubmissionRaw {
        val signer = signerProvider.rootSignerFor(metaAccount)

        val requestedOrigin = metaAccount.requireAccountIdIn(chain)
        val actualOrigin = signer.signerAccountId(chain)

        val submissionOrigin = SubmissionOrigin(requestedOrigin = requestedOrigin, actualOrigin = actualOrigin)

        val extrinsicBuilder = extrinsicBuilderFactory.create(chain, signer, requestedOrigin)

        extrinsicBuilder.formExtrinsic(submissionOrigin)

        val extrinsic = extrinsicBuilder.build(useBatchAll = true)

        return SubmissionRaw(extrinsic, submissionOrigin)
    }

    private suspend fun getFeeSigner(chain: Chain, origin: TransactionOrigin): NovaSigner {
        val metaAccount = accountRepository.requireMetaAccountFor(origin, chain.id)
        return signerProvider.feeSigner(metaAccount, chain)
    }

    private data class SubmissionRaw(val extrinsicRaw: String, val submissionOrigin: SubmissionOrigin)
}
