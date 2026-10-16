package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId

interface BittensorPositionSource {

    suspend fun positions(chainId: String, coldKey: AccountId): BittensorStakePositions
}

class RealBittensorPositionSource(
    private val runtimeCallsApi: MultiChainRuntimeCallsApi
) : BittensorPositionSource {

    override suspend fun positions(chainId: String, coldKey: AccountId): BittensorStakePositions {
        return runtimeCallsApi.forChain(chainId).call(
            section = "StakeInfoRuntimeApi",
            method = "get_stake_info_for_coldkey",
            arguments = mapOf("coldkey_account" to coldKey),
            returnBinding = ::bindBittensorPositions
        )
    }
}

internal fun bindBittensorPositions(decoded: Any?): BittensorStakePositions {
    val root = mutableListOf<BittensorRootPosition>()
    val subnets = mutableListOf<BittensorSubnetPosition>()
    bindList(decoded) { item ->
        val value = item.castToStruct()
        val netuid = BittensorNetuid.from(bindBittensorU16(value["netuid"]))
        val stake = bindBittensorU64(value["stake"])
        if (stake.isPositive()) {
            val hotkey = bindAccountIdKey(value["hotkey"])
            when (netuid) {
                BittensorNetuid.Root -> root += BittensorRootPosition(hotkey, stake)
                is BittensorNetuid.Subnet -> subnets += BittensorSubnetPosition(netuid, hotkey, stake, value.get<Any?>("isRegistered")?.let(::bindBoolean))
            }
        }
    }
    return BittensorStakePositions(root, subnets)
}

internal fun bindBittensorRootPositions(decoded: Any?): List<BittensorRootPosition> = bindBittensorPositions(decoded).root

internal fun bindBittensorSubnetPositions(decoded: Any?): List<BittensorSubnetPosition> = bindBittensorPositions(decoded).subnets
