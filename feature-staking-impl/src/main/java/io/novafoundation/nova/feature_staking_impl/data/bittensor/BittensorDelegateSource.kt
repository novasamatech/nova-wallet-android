package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class BittensorDelegateSource(private val runtimeCalls: MultiChainRuntimeCallsApi) {

    /** Subnets where [hotkey] holds a validator permit; null when the hotkey is not a registered delegate. */
    suspend fun validatorPermits(chainId: ChainId, hotkey: AccountIdKey): Set<BittensorNetuid.Subnet>? = runtimeCalls.forChain(chainId)
        .call(
            section = "DelegateInfoRuntimeApi",
            method = "get_delegate",
            arguments = mapOf("delegate_account" to hotkey.value),
            returnBinding = ::bindBittensorDelegatePermits
        )
}

internal fun bindBittensorDelegatePermits(decoded: Any?): Set<BittensorNetuid.Subnet>? {
    val delegate = decoded?.castToStruct() ?: return null
    // The SDK normalizes composite fields from metadata to camelCase.
    return bindList(delegate.get<Any?>("validatorPermits")) { BittensorNetuid.from(bindBittensorU16(it)) }
        .filterIsInstance<BittensorNetuid.Subnet>()
        .toSet()
}
