package io.novafoundation.nova.runtime.network.rpc

import io.novafoundation.nova.common.data.network.runtime.binding.BlockNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindWeight
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.data.network.runtime.calls.FeeCalculationRequest
import io.novafoundation.nova.common.data.network.runtime.calls.GetBlockHashRequest
import io.novafoundation.nova.common.data.network.runtime.calls.GetBlockRequest
import io.novafoundation.nova.common.data.network.runtime.calls.GetFinalizedHeadRequest
import io.novafoundation.nova.common.data.network.runtime.calls.GetHeaderRequest
import io.novafoundation.nova.common.data.network.runtime.calls.NextAccountIndexRequest
import io.novafoundation.nova.common.data.network.runtime.model.FeeResponse
import io.novafoundation.nova.common.data.network.runtime.model.SignedBlock
import io.novafoundation.nova.common.data.network.runtime.model.SignedBlock.Block.Header
import io.novafoundation.nova.common.utils.extrinsicHash
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicStatus
import io.novafoundation.nova.runtime.extrinsic.asExtrinsicStatus
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime
import jp.co.soramitsu.fearless_utils.extensions.fromHex
import jp.co.soramitsu.fearless_utils.wsrpc.executeAsync
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.nonNull
import jp.co.soramitsu.fearless_utils.wsrpc.mappers.pojo
import jp.co.soramitsu.fearless_utils.wsrpc.request.DeliveryType
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.author.SubmitAndWatchExtrinsicRequest
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.author.SubmitExtrinsicRequest
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersion
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.chain.RuntimeVersionRequest
import jp.co.soramitsu.fearless_utils.wsrpc.subscriptionFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigInteger

private const val FEE_DECODE_TYPE = "RuntimeDispatchInfo"

@Suppress("EXPERIMENTAL_API_USAGE")
class RpcCalls(
    private val chainRegistry: ChainRegistry,
    private val runtimeCallsApi: MultiChainRuntimeCallsApi
) {

    suspend fun getExtrinsicFee(chainId: ChainId, extrinsic: String): FeeResponse {
        val runtime = chainRegistry.getRuntime(chainId)

        return if (runtime.typeRegistry[FEE_DECODE_TYPE] != null) {
            val lengthInBytes = extrinsic.fromHex().size

            runtimeCallsApi.forChain(chainId).call(
                section = "TransactionPaymentApi",
                method = "query_info",
                arguments = listOf(
                    extrinsic to null,
                    lengthInBytes.toBigInteger() to "u32"
                ),
                returnType = FEE_DECODE_TYPE,
                returnBinding = ::bindPartialFee
            )
        } else {
            val request = FeeCalculationRequest(extrinsic)
            socketFor(chainId).executeAsync(request, mapper = pojo<FeeResponse>().nonNull())
        }
    }

    suspend fun submitExtrinsic(chainId: ChainId, extrinsic: String): String {
        val request = SubmitExtrinsicRequest(extrinsic)

        return socketFor(chainId).executeAsync(
            request,
            mapper = pojo<String>().nonNull(),
            deliveryType = DeliveryType.AT_MOST_ONCE
        )
    }

    fun submitAndWatchExtrinsic(chainId: ChainId, extrinsic: String): Flow<ExtrinsicStatus> {
        return flow {
            val hash = extrinsic.extrinsicHash()
            val request = SubmitAndWatchExtrinsicRequest(extrinsic)

            val inner = socketFor(chainId).subscriptionFlow(request, unsubscribeMethod = "author_unwatchExtrinsic")
                .map { it.asExtrinsicStatus(hash) }

            emitAll(inner)
        }
    }

    suspend fun getNonce(chainId: ChainId, accountAddress: String): BigInteger {
        val nonceRequest = NextAccountIndexRequest(accountAddress)

        val response = socketFor(chainId).executeAsync(nonceRequest)
        val doubleResult = response.result as Double

        return doubleResult.toInt().toBigInteger()
    }

    suspend fun getRuntimeVersion(chainId: ChainId): RuntimeVersion {
        val request = RuntimeVersionRequest()

        return socketFor(chainId).executeAsync(request, mapper = pojo<RuntimeVersion>().nonNull())
    }

    /**
     * Retrieves the block with given hash
     * If hash is null, than the latest block is returned
     */
    suspend fun getBlock(chainId: ChainId, hash: String? = null): SignedBlock {
        val blockRequest = GetBlockRequest(hash)

        return socketFor(chainId).executeAsync(blockRequest, mapper = pojo<SignedBlock>().nonNull())
    }

    /**
     * Get hash of the last finalized block in the canon chain
     */
    suspend fun getFinalizedHead(chainId: ChainId): String {
        return socketFor(chainId).executeAsync(GetFinalizedHeadRequest, mapper = pojo<String>().nonNull())
    }

    /**
     * Retrieves the header for a specific block
     *
     * @param hash - hash of the block. If null - then the  best pending header is returned
     */
    suspend fun getBlockHeader(chainId: ChainId, hash: String? = null): Header {
        return socketFor(chainId).executeAsync(GetHeaderRequest(hash), mapper = pojo<Header>().nonNull())
    }

    /**
     * Retrieves the hash of a specific block
     *
     *  @param blockNumber - if null, then the  best block hash is returned
     */
    suspend fun getBlockHash(chainId: ChainId, blockNumber: BlockNumber? = null): String {
        return socketFor(chainId).executeAsync(GetBlockHashRequest(blockNumber), mapper = pojo<String>().nonNull())
    }

    private fun socketFor(chainId: ChainId) = chainRegistry.getConnection(chainId).socketService

    private fun bindPartialFee(decoded: Any?): FeeResponse {
        val asStruct = decoded.castToStruct()

        return FeeResponse(
            partialFee = bindNumber(asStruct["partialFee"]),
            weight = bindWeight(asStruct["weight"])
        )
    }
}
