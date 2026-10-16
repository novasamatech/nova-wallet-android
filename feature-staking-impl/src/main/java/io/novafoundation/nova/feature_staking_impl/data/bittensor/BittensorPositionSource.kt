package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi

interface BittensorPositionSource {

    suspend fun rootPositions(chainId: String, coldkey: ByteArray): List<BittensorRootPosition>

    suspend fun subnetPositions(chainId: String, coldkey: ByteArray): List<BittensorSubnetPosition>
}

class RealBittensorPositionSource(
    private val runtimeCallsApi: MultiChainRuntimeCallsApi
) : BittensorPositionSource {

    override suspend fun rootPositions(chainId: String, coldkey: ByteArray): List<BittensorRootPosition> {
        return runtimeCallsApi.forChain(chainId).call(
            section = "StakeInfoRuntimeApi",
            method = "get_stake_info_for_coldkey",
            arguments = mapOf("coldkey_account" to coldkey),
            returnBinding = ::bindBittensorRootPositions
        )
    }

    override suspend fun subnetPositions(chainId: String, coldkey: ByteArray): List<BittensorSubnetPosition> {
        return runtimeCallsApi.forChain(chainId).call(
            section = "StakeInfoRuntimeApi",
            method = "get_stake_info_for_coldkey",
            arguments = mapOf("coldkey_account" to coldkey),
            returnBinding = ::bindBittensorSubnetPositions
        )
    }
}

internal fun bindBittensorRootPositions(decoded: Any?): List<BittensorRootPosition> = bindList(decoded) { item ->
    val value = item.castToStruct()
    val netuid = bindNumber(value["netuid"])
    val stake = bindNumber(value["stake"])

    if (netuid.signum() == 0 && stake.signum() > 0) {
        BittensorRootPosition(bindAccountIdKey(value["hotkey"]), stake)
    } else {
        null
    }
}.filterNotNull()

internal fun bindBittensorSubnetPositions(decoded: Any?): List<BittensorSubnetPosition> = bindList(decoded) { item ->
    val value = item.castToStruct()
    val netuid = bindBittensorU16(value["netuid"])
    val stake = bindBittensorU64(value["stake"])

    if (netuid != 0 && stake.signum() > 0) {
        BittensorSubnetPosition(
            BittensorSubnetId(netuid),
            bindAccountIdKey(value["hotkey"]),
            stake,
            value.get<Any?>("isRegistered")?.let(::bindBoolean)
        )
    } else {
        null
    }
}.filterNotNull()
