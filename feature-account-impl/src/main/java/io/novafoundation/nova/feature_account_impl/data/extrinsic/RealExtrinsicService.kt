package io.novafoundation.nova.feature_account_impl.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult
import io.novafoundation.nova.common.utils.multiResult.runMultiCatching
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sum
import io.novafoundation.nova.common.utils.takeWhileInclusive
import io.novafoundation.nova.common.utils.tip
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsic
import io.novafoundation.nova.feature_account_api.data.extrinsic.FormMultiExtrinsicWithOrigin
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.InlineFee
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.extrinsic.multi.SimpleCallBuilder
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.fromHex
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Extrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
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
) : ExtrinsicService {

    override suspend fun submitMultiExtrinsicWithSelectedWalletAwaitingInclusion(
        chain: Chain,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): RetriableMultiResult<ExtrinsicStatus.InBlock> {
        return runMultiCatching(
            intermediateListLoading = { constructSplitExtrinsicsForSubmission(chain, formExtrinsic) },
            listProcessing = { extrinsic ->
                rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
                    .filterIsInstance<ExtrinsicStatus.InBlock>()
                    .first()
            }
        )
    }

    override suspend fun submitExtrinsicWithSelectedWalletV2(
        chain: Chain,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Result<ExtrinsicSubmission> {
        val account = accountRepository.getSelectedMetaAccount()
        val accountId = account.requireAccountIdIn(chain)

        return submitExtrinsicWithAnySuitableWalletV2(chain, accountId, formExtrinsic)
    }

    override suspend fun submitAndWatchExtrinsicWithSelectedWallet(
        chain: Chain,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Flow<ExtrinsicStatus> {
        val account = accountRepository.getSelectedMetaAccount()
        val accountId = account.accountIdIn(chain)!!

        return submitAndWatchExtrinsicAnySuitableWallet(chain, accountId, formExtrinsic)
    }

    override suspend fun submitExtrinsicWithAnySuitableWallet(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Result<String> = submitExtrinsicWithAnySuitableWalletV2(chain, accountId, formExtrinsic)
        .map { it.hash }

    private suspend fun submitExtrinsicWithAnySuitableWalletV2(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Result<ExtrinsicSubmission> = runCatching {
        val extrinsic = buildExtrinsic(chain, accountId, formExtrinsic)
        val hash = rpcCalls.submitExtrinsic(chain.id, extrinsic)
        ExtrinsicSubmission(hash, accountId)
    }

    override suspend fun submitAndWatchExtrinsicAnySuitableWallet(
        chain: Chain,
        accountId: ByteArray,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): Flow<ExtrinsicStatus> {
        val extrinsic = buildExtrinsic(chain, accountId, formExtrinsic)

        return rpcCalls.submitAndWatchExtrinsic(chain.id, extrinsic)
            .takeWhileInclusive { !it.terminal }
    }

    override suspend fun paymentInfo(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): FeeResponse {
        val extrinsic = extrinsicBuilderFactory.createForFee(chain)
            .also { it.formExtrinsic() }
            .build()

        return rpcCalls.getExtrinsicFee(chain, extrinsic)
    }

    override suspend fun estimateFee(
        chain: Chain,
        formExtrinsic: suspend ExtrinsicBuilder.() -> Unit,
    ): BigInteger {
        val extrinsicBuilder = extrinsicBuilderFactory.createForFee(chain)
        extrinsicBuilder.formExtrinsic()
        val extrinsic = extrinsicBuilder.build()

        return estimateFee(chain, extrinsic).amount
    }

    override suspend fun estimateFeeV2(chain: Chain, formExtrinsic: suspend ExtrinsicBuilder.() -> Unit): Fee {
        return InlineFee(estimateFee(chain, formExtrinsic))
    }

    override suspend fun estimateFee(chain: Chain, extrinsic: String): Fee {
        val chainId = chain.id
        val baseFee = rpcCalls.getExtrinsicFee(chain, extrinsic).partialFee

        val runtime = chainRegistry.getRuntime(chainId)

        val decodedExtrinsic = Extrinsic.fromHex(runtime, extrinsic)

        val tip = decodedExtrinsic.tip().orZero()

        return InlineFee(tip + baseFee)
    }

    override suspend fun estimateMultiFee(chain: Chain, formExtrinsic: FormMultiExtrinsic): BigInteger {
        val feeExtrinsicBuilderSequence = extrinsicBuilderFactory.createMultiForFee(chain)

        val extrinsics = constructSplitExtrinsics(chain, formExtrinsic, feeExtrinsicBuilderSequence)

        val separateFees = extrinsics.map { estimateFee(chain, it).amount }

        return separateFees.sum()
    }

    private suspend fun constructSplitExtrinsicsForSubmission(
        chain: Chain,
        formExtrinsic: FormMultiExtrinsicWithOrigin
    ): List<String> {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val signer = signerProvider.signerFor(metaAccount)
        val accountId = metaAccount.requireAccountIdIn(chain)

        val extrinsicBuilderSequence = extrinsicBuilderFactory.createMulti(chain, signer, accountId)

        val formExtrinsicWithOrigin: FormMultiExtrinsic = { formExtrinsic(accountId) }

        return constructSplitExtrinsics(chain, formExtrinsicWithOrigin, extrinsicBuilderSequence)
    }

    private suspend fun constructSplitExtrinsics(
        chain: Chain,
        formExtrinsic: FormMultiExtrinsic,
        extrinsicBuilderSequence: Sequence<ExtrinsicBuilder>,
    ): List<String> = coroutineScope {
        val runtime = chainRegistry.getRuntime(chain.id)

        val callBuilder = SimpleCallBuilder(runtime).apply { formExtrinsic() }
        val splitCalls = extrinsicSplitter.split(callBuilder, chain)

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
        accountId: ByteArray,
        formExtrinsic: FormExtrinsicWithOrigin,
    ): String {
        val metaAccount = accountRepository.findMetaAccount(accountId) ?: error("No meta account found accessing ${accountId.toHexString()}")
        val signer = signerProvider.signerFor(metaAccount)

        val extrinsicBuilder = extrinsicBuilderFactory.create(chain, signer, accountId)

        extrinsicBuilder.formExtrinsic(accountId)

        return extrinsicBuilder.build(useBatchAll = true)
    }
}
