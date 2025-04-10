package io.novafoundation.nova.feature_account_impl.data.extrinsic

import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.data.network.runtime.binding.fitsIn
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.extrinsic.CustomTransactionExtensions
import io.novafoundation.nova.runtime.extrinsic.multi.CallBuilder
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.binding.total
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.BlockLimitsRepository
import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.Era
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.BatchMode
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SendableExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.ChargeTransactionPayment
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckGenesis
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckMortality
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckSpecVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.CheckTxVersion
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHash
import io.novasama.substrate_sdk_android.runtime.extrinsic.v5.transactionExtension.extensions.checkMetadataHash.CheckMetadataHashMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.math.BigInteger
import javax.inject.Inject

typealias SplitCalls = List<List<GenericCall.Instance>>

interface ExtrinsicSplitter {

    suspend fun split(signer: NovaSigner, callBuilder: CallBuilder, chain: Chain): SplitCalls
}

private typealias CallWeightsByType = Map<String, Deferred<WeightV2>>

private const val LEAVE_SOME_SPACE_MULTIPLIER = 0.8

@FeatureScope
internal class RealExtrinsicSplitter @Inject constructor(
    private val rpcCalls: RpcCalls,
    private val blockLimitsRepository: BlockLimitsRepository,
    private val signingContextFactory: SigningContext.Factory,
) : ExtrinsicSplitter {

    override suspend fun split(signer: NovaSigner, callBuilder: CallBuilder, chain: Chain): SplitCalls = coroutineScope {
        val weightByCallId = estimateWeightByCallType(signer, callBuilder, chain)

        val blockLimit = blockLimitsRepository.maxWeightForNormalExtrinsics(chain.id)
        val lastBlockWeight = blockLimitsRepository.lastBlockWeight(chain.id).total()
        val remainingLimit = (blockLimit - lastBlockWeight) * LEAVE_SOME_SPACE_MULTIPLIER

        val signerLimit = signer.maxCallsPerTransaction()

        callBuilder.splitCallsWith(weightByCallId, remainingLimit, signerLimit)
    }

    private val GenericCall.Instance.uniqueId: String
        get() {
            val (moduleIdx, functionIdx) = function.index
            return "$moduleIdx:$functionIdx"
        }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun CoroutineScope.estimateWeightByCallType(signer: NovaSigner, callBuilder: CallBuilder, chain: Chain): CallWeightsByType {
        return callBuilder.calls.groupBy { it.uniqueId }
            .mapValues { (_, calls) ->
                val sample = calls.first()
                val sampleExtrinsic = wrapInFakeExtrinsic(signer, sample, callBuilder.runtime, chain)

                async { rpcCalls.getExtrinsicFee(chain, sampleExtrinsic).weight }
            }
    }

    private suspend fun CallBuilder.splitCallsWith(
        weights: CallWeightsByType,
        blockWeightLimit: WeightV2,
        signerNumberOfCallsLimit: Int?,
    ): SplitCalls {
        val split = mutableListOf<List<GenericCall.Instance>>()

        var currentBatch = mutableListOf<GenericCall.Instance>()
        var currentBatchWeight: WeightV2 = WeightV2.zero()

        calls.forEach { call ->
            val estimatedCallWeight = weights.getValue(call.uniqueId).await()
            val newWeight = currentBatchWeight + estimatedCallWeight
            val exceedsByWeight = !newWeight.fitsIn(blockWeightLimit)
            val exceedsByNumberOfCalls = signerNumberOfCallsLimit != null && currentBatch.size >= signerNumberOfCallsLimit

            if (exceedsByWeight || exceedsByNumberOfCalls) {
                if (!estimatedCallWeight.fitsIn(blockWeightLimit)) throw IllegalArgumentException("Impossible to fit call $call into a block")

                split += currentBatch

                currentBatchWeight = estimatedCallWeight
                currentBatch = mutableListOf(call)
            } else {
                currentBatchWeight += estimatedCallWeight
                currentBatch += call
            }
        }

        if (currentBatch.isNotEmpty()) {
            split.add(currentBatch)
        }

        return split
    }

    private suspend fun wrapInFakeExtrinsic(
        signer: NovaSigner,
        call: GenericCall.Instance,
        runtime: RuntimeSnapshot,
        chain: Chain
    ): SendableExtrinsic {
        val genesisHash = chain.requireGenesisHash().fromHex()

        return ExtrinsicBuilder(
            runtime = runtime,
            extrinsicVersion = ExtrinsicVersion.V4,
            batchMode = BatchMode.BATCH,
        ).apply {
            setTransactionExtension(CheckMortality(Era.Immortal, genesisHash))
            setTransactionExtension(CheckGenesis(chain.requireGenesisHash().fromHex()))
            setTransactionExtension(ChargeTransactionPayment(BigInteger.ZERO))
            setTransactionExtension(CheckMetadataHash(CheckMetadataHashMode.Disabled))
            setTransactionExtension(CheckSpecVersion(0))
            setTransactionExtension(CheckTxVersion(0))

            CustomTransactionExtensions.defaultValues().forEach(::setTransactionExtension)

            call(call)

            val signingContext = signingContextFactory.default(chain)
            signer.setSignerDataForFee(signingContext)
        }.buildExtrinsic()
    }
}
