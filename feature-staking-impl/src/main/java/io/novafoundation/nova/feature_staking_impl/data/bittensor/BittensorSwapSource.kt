package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapSimulation
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import java.math.BigInteger

interface BittensorSwapSource {

    suspend fun simulate(
        chainId: String,
        subnetId: BittensorSubnetId,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapSimulation

    /** Current marginal price in rao per one alpha; not the average price of a simulation. */
    suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorSubnetId): BigInteger
}

class RealBittensorSwapSource(
    private val runtimeCallsApi: MultiChainRuntimeCallsApi
) : BittensorSwapSource {

    override suspend fun simulate(
        chainId: String,
        subnetId: BittensorSubnetId,
        direction: BittensorSwapDirection,
        amountIn: BigInteger
    ): BittensorSwapSimulation {
        require(amountIn.signum() > 0 && amountIn.bitLength() <= 64) { "Swap input must be a positive u64" }
        val buying = direction == BittensorSwapDirection.TAO_TO_ALPHA

        return runtimeCallsApi.forChain(chainId).call(
            section = "SwapRuntimeApi",
            method = if (buying) "sim_swap_tao_for_alpha" else "sim_swap_alpha_for_tao",
            arguments = mapOf(
                "netuid" to BigInteger.valueOf(subnetId.value.toLong()),
                (if (buying) "tao" else "alpha") to amountIn
            ),
            returnBinding = { bindBittensorSwapSimulation(it, subnetId, direction, amountIn) }
        )
    }

    override suspend fun currentAlphaPrice(chainId: String, subnetId: BittensorSubnetId): BigInteger {
        return runtimeCallsApi.forChain(chainId).call(
            section = "SwapRuntimeApi",
            method = "current_alpha_price",
            arguments = mapOf("netuid" to BigInteger.valueOf(subnetId.value.toLong())),
            returnBinding = { bindBittensorU64(it).also { price -> require(price.signum() > 0) { "Subnet has no valid alpha price" } } }
        )
    }
}

internal fun bindBittensorSwapSimulation(
    decoded: Any?,
    subnetId: BittensorSubnetId,
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
    require(amountIn.signum() > 0 && amountOut.signum() > 0) { "Subnet swap simulation failed" }
    val inputFee = if (buying) taoFee else alphaFee
    require(amountIn + inputFee <= requestedAmount) { "Simulation input including fee exceeds the requested amount" }

    return BittensorSwapSimulation(
        subnetId = subnetId,
        direction = direction,
        amountIn = amountIn,
        amountOut = amountOut,
        taoFee = taoFee,
        alphaFee = alphaFee,
        taoSlippage = bindBittensorU64(value["taoSlippage"]),
        alphaSlippage = bindBittensorU64(value["alphaSlippage"])
    )
}

internal fun bindBittensorU64(decoded: Any?): BigInteger = bindNumber(decoded).also {
    require(it.signum() >= 0 && it.bitLength() <= 64) { "Invalid Bittensor u64 value" }
}
