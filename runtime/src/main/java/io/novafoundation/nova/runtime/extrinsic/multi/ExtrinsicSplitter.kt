package io.novafoundation.nova.runtime.extrinsic.multi

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.common.utils.times
import io.novafoundation.nova.runtime.ext.requireGenesisHash
import io.novafoundation.nova.runtime.extrinsic.CustomSignedExtensions
import io.novafoundation.nova.runtime.extrinsic.FeeSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.repository.BlockLimitsRepository
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Era
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.ExtrinsicBuilder
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

typealias SplitCalls = List<List<GenericCall.Instance>>

interface ExtrinsicSplitter {

    suspend fun split(callBuilder: CallBuilder, chain: Chain): SplitCalls
}

private typealias CallWeightsByType = Map<String, Deferred<Weight>>
private const val LEAVE_SOME_SPACE_MULTIPLIER = 0.8

internal class RealExtrinsicSplitter(
    private val rpcCalls: RpcCalls,
    private val blockLimitsRepository: BlockLimitsRepository,
) : ExtrinsicSplitter {

    override suspend fun split(callBuilder: CallBuilder, chain: Chain): SplitCalls = coroutineScope {
        val weightByCallId = estimateWeightByCallType(callBuilder, chain)

        val blockLimit = blockLimitsRepository.maxWeightForNormalExtrinsics(chain.id) * LEAVE_SOME_SPACE_MULTIPLIER

        callBuilder.splitCallsWith(weightByCallId, blockLimit)
    }

    private val GenericCall.Instance.uniqueId: String
        get() {
            val (moduleIdx, functionIdx) = function.index

            return "$moduleIdx:$functionIdx"
        }

    @Suppress("SuspendFunctionOnCoroutineScope")
    private suspend fun CoroutineScope.estimateWeightByCallType(callBuilder: CallBuilder, chain: Chain): CallWeightsByType {
        return callBuilder.calls.groupBy { it.uniqueId }
            .mapValues { (_, calls) ->
                val sample = calls.first()
                val sampleExtrinsic = wrapInFakeExtrinsic(sample, callBuilder.runtime, chain)

                async { rpcCalls.getExtrinsicFee(chain.id, sampleExtrinsic).weight }
            }
    }

    private suspend fun CallBuilder.splitCallsWith(weights: CallWeightsByType, blockLimit: Weight): SplitCalls {
        val split = mutableListOf<List<GenericCall.Instance>>()

        var currentBatch = mutableListOf<GenericCall.Instance>()
        var currentBatchWeight: Weight = Weight.ZERO

        calls.forEach { call ->
            val estimatedCallWeight = weights.getValue(call.uniqueId).await()

            if (currentBatchWeight + estimatedCallWeight > blockLimit) {
                if (estimatedCallWeight > blockLimit) throw IllegalArgumentException("Impossible to fit call $call into a block")

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

    private suspend fun wrapInFakeExtrinsic(call: GenericCall.Instance, runtime: RuntimeSnapshot, chain: Chain): String {
        val signer = FeeSigner(chain)
        val genesisHash = chain.requireGenesisHash().fromHex()

        return ExtrinsicBuilder(
            tip = BalanceOf.ZERO,
            runtime = runtime,
            nonce = BalanceOf.ZERO,
            runtimeVersion = RuntimeVersion(specVersion = 0, transactionVersion = 0),
            genesisHash = genesisHash,
            blockHash = genesisHash,
            era = Era.Immortal,
            customSignedExtensions = CustomSignedExtensions.extensionsWithValues(),
            signer = signer,
            accountId = signer.accountId()
        )
            .call(call)
            .build()
    }
}
