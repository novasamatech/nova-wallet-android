package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isZero
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novasama.substrate_sdk_android.runtime.AccountId

interface BittensorPositionSource {

    suspend fun rootPositions(chainId: String, coldKey: AccountId): List<BittensorRootPosition>

    suspend fun subnetPositions(chainId: String, coldKey: AccountId): List<BittensorSubnetPosition>
}

class RealBittensorPositionSource(
    private val runtimeCallsApi: MultiChainRuntimeCallsApi
) : BittensorPositionSource {

    override suspend fun rootPositions(chainId: String, coldKey: AccountId): List<BittensorRootPosition> {
        return runtimeCallsApi.forChain(chainId).call(
            section = "StakeInfoRuntimeApi",
            method = "get_stake_info_for_coldkey",
            arguments = mapOf("coldkey_account" to coldKey),
            returnBinding = ::bindBittensorRootPositions
        )
    }

    override suspend fun subnetPositions(chainId: String, coldKey: AccountId): List<BittensorSubnetPosition> {
        return runtimeCallsApi.forChain(chainId).call(
            section = "StakeInfoRuntimeApi",
            method = "get_stake_info_for_coldkey",
            arguments = mapOf("coldkey_account" to coldKey),
            returnBinding = ::bindBittensorSubnetPositions
        )
    }
}

internal fun bindBittensorRootPositions(decoded: Any?): List<BittensorRootPosition> = bindList(decoded) { item ->
    val value = item.castToStruct()
    val netuid = bindNumber(value["netuid"])
    val stake = bindNumber(value["stake"])

    if (netuid.isZero && stake.isPositive()) {
        BittensorRootPosition(bindAccountIdKey(value["hotkey"]), stake)
    } else {
        null
    }
}.filterNotNull()

internal fun bindBittensorSubnetPositions(decoded: Any?): List<BittensorSubnetPosition> = bindList(decoded) { item ->
    val value = item.castToStruct()
    val netuid = bindBittensorU16(value["netuid"])
    val stake = bindBittensorU64(value["stake"])

    if (netuid != 0 && stake.isPositive()) {
        BittensorSubnetPosition(BittensorSubnetId(netuid), bindAccountIdKey(value["hotkey"]), stake)
    } else {
        null
    }
}.filterNotNull()
