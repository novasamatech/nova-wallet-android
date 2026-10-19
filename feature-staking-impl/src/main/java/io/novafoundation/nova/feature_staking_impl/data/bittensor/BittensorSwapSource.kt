package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapSimulation
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

interface BittensorSwapSource {

    /** Simulation and marginal price from one captured best block. */
    suspend fun quote(
        chainId: String,
        subnetId: BittensorNetuid.Subnet,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapQuote

    suspend fun simulate(
        chainId: String,
        subnetId: BittensorNetuid.Subnet,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapSimulation

    /** Current marginal price in rao per one alpha; not the average price of a simulation. */
    suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorNetuid.Subnet): BigInteger
}

data class BittensorSwapQuote(
    val blockHash: String,
    val simulation: BittensorSwapSimulation,
    val spotPrice: BigInteger
)

class RealBittensorSwapSource(
    private val runtimeCallsApi: MultiChainRuntimeCallsApi,
    private val rpcCalls: RpcCalls
) : BittensorSwapSource {

    override suspend fun quote(
        chainId: String,
        subnetId: BittensorNetuid.Subnet,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapQuote = coroutineScope {
        require(amountIn.isPositive() && amountIn.bitLength() <= 64) { "Swap input must be a positive u64" }
        val blockHash = rpcCalls.getBlockHash(chainId)
        val runtimeApi = runtimeCallsApi.forChain(chainId)
        val price = async { priceAtBlock(runtimeApi, blockHash, subnetId) }
        val simulation = simulateAtBlock(runtimeApi, blockHash, subnetId, direction, amountIn)
        BittensorSwapQuote(blockHash, simulation, price.await())
    }

    override suspend fun simulate(
        chainId: String,
        subnetId: BittensorNetuid.Subnet,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapSimulation {
        require(amountIn.isPositive() && amountIn.bitLength() <= 64) { "Swap input must be a positive u64" }
        val buying = direction == BittensorSwapDirection.TAO_TO_ALPHA

        return runtimeCallsApi.forChain(chainId).call(
            section = "SwapRuntimeApi",
            method = if (buying) "sim_swap_tao_for_alpha" else "sim_swap_alpha_for_tao",
            arguments = simulationArguments(subnetId, direction, amountIn),
            returnBinding = { bindBittensorSwapSimulation(it, subnetId, direction, amountIn) }
        )
    }

    override suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorNetuid.Subnet): BigInteger {
        return runtimeCallsApi.forChain(chainId).call(
            section = "SwapRuntimeApi",
            method = "current_alpha_price",
            arguments = mapOf("netuid" to BigInteger.valueOf(subnetId.value.toLong())),
            returnBinding = ::bindAlphaPrice
        )
    }

    private fun bindAlphaPrice(decoded: Any?): BigInteger {
        val price = bindBittensorU64(decoded)
        require(price.isPositive()) { "Subnet has no valid alpha price" }
        return price
    }

    private suspend fun simulateAtBlock(
        runtimeApi: RuntimeCallsApi,
        blockHash: String,
        subnetId: BittensorNetuid.Subnet,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapSimulation {
        val buying = direction == BittensorSwapDirection.TAO_TO_ALPHA
        return runtimeApi.callAt(
            blockHash = blockHash,
            section = "SwapRuntimeApi",
            method = if (buying) "sim_swap_tao_for_alpha" else "sim_swap_alpha_for_tao",
            arguments = simulationArguments(subnetId, direction, amountIn),
            returnBinding = { bindBittensorSwapSimulation(it, subnetId, direction, amountIn) }
        )
    }

    private suspend fun priceAtBlock(
        runtimeApi: RuntimeCallsApi,
        blockHash: String,
        subnetId: BittensorNetuid.Subnet
    ): BigInteger {
        return runtimeApi.callAt(
            blockHash = blockHash,
            section = "SwapRuntimeApi",
            method = "current_alpha_price",
            arguments = mapOf("netuid" to BigInteger.valueOf(subnetId.value.toLong())),
            returnBinding = ::bindAlphaPrice
        )
    }

    private fun simulationArguments(
        subnetId: BittensorNetuid.Subnet,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): Map<String, Any?> {
        val buying = direction == BittensorSwapDirection.TAO_TO_ALPHA
        return mapOf(
            "netuid" to BigInteger.valueOf(subnetId.value.toLong()),
            (if (buying) "tao" else "alpha") to amountIn
        )
    }
}

internal fun bindBittensorSwapSimulation(
    decoded: Any?,
    subnetId: BittensorNetuid.Subnet,
    direction: BittensorSwapDirection,
    requestedAmount: BigInteger
): BittensorSwapSimulation {
    val value = decoded.castToStruct()
    val taoAmount = bindBittensorU64(value["taoAmount"])
    val alphaAmount = bindBittensorU64(value["alphaAmount"])
    val taoFee = bindBittensorU64(value["taoFee"])
    val alphaFee = bindBittensorU64(value["alphaFee"])
    val buying = direction == BittensorSwapDirection.TAO_TO_ALPHA
    val amountIn = if (buying) taoAmount else alphaAmount
    val amountOut = if (buying) alphaAmount else taoAmount

    // Runtime reports simulation failures as an all-zero struct, not an RPC error.
    require(amountIn.isPositive() && amountOut.isPositive()) { "Subnet swap simulation failed" }
    val inputFee = if (buying) taoFee else alphaFee
    require(amountIn + inputFee <= requestedAmount) { "Simulation input including fee exceeds the requested amount" }

    return BittensorSwapSimulation(
        subnetId = subnetId,
        direction = direction,
        amountIn = amountIn,
        amountOut = amountOut,
        taoFee = taoFee,
        alphaFee = alphaFee,
        taoSlippage = value.get<Any?>("taoSlippage")?.let(::bindBittensorU64),
        alphaSlippage = value.get<Any?>("alphaSlippage")?.let(::bindBittensorU64)
    )
}
