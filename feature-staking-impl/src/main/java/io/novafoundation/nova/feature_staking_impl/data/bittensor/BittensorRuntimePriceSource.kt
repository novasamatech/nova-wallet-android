package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

class BittensorRuntimePriceSource(private val runtimeCalls: MultiChainRuntimeCallsApi) {

    suspend fun prices(chainId: ChainId): Map<BittensorNetuid.Subnet, BigInteger> = runtimeCalls.forChain(chainId)
        .call(
            section = "SwapRuntimeApi",
            method = "current_alpha_price_all",
            arguments = emptyMap(),
            returnBinding = ::bindBittensorRuntimePrices
        )
}

internal fun bindBittensorRuntimePrices(decoded: Any?): Map<BittensorNetuid.Subnet, BigInteger> {
    val rows = bindList(decoded) { it.castToStruct() }
    val seenSubnets = mutableSetOf<BittensorNetuid>()
    return buildMap {
        for (row in rows) {
            val netuid = BittensorNetuid.from(bindBittensorU16(row["netuid"]))
            require(seenSubnets.add(netuid)) { "Duplicate subnet price" }
            val price = bindBittensorU64(row["price"])
            if (netuid is BittensorNetuid.Subnet && price.isPositive()) {
                put(netuid, price)
            }
        }
    }
}
