package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.hash.isPositive
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.getSocket
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.runtime.metadata.createRequest
import io.novasama.substrate_sdk_android.runtime.metadata.decodeOutput
import io.novasama.substrate_sdk_android.runtime.metadata.method
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.runtimeApi
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojo
import io.novasama.substrate_sdk_android.wsrpc.request.runtime.RuntimeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import java.math.BigInteger

interface BittensorExitRuntimeSource {

    suspend fun preflight(chainId: String, coldkey: AccountId, hotkey: AccountId, netuid: BittensorNetuid): BittensorExitPreflight
}

class RealBittensorExitRuntimeSource(
    private val storage: StorageDataSource,
    private val rpc: RpcCalls,
    private val chains: ChainRegistry
) : BittensorExitRuntimeSource {

    override suspend fun preflight(chainId: String, coldkey: AccountId, hotkey: AccountId, netuid: BittensorNetuid): BittensorExitPreflight = withTimeout(
        25_000
    ) {
        val hash = rpc.getBlockHash(chainId)
        val socket = chains.getSocket(chainId)
        storage.query(chainId, at = hash, applyStorageDefault = true) {
            // Storage and both runtime APIs use the same block and its runtime metadata.
            suspend fun call(method: String, values: List<Any?>): Any? {
                val apiMethod = runtime.metadata.runtimeApi("StakeInfoRuntimeApi").method(method)
                require(apiMethod.inputs.size == values.size)
                val args = apiMethod.inputs.zip(values).associate { (input, value) -> input.name to value }
                val request = apiMethod.createRequest(runtime, args)
                val atBlock = RuntimeRequest(request.method, request.params + hash)
                val encoded = requireNotNull(socket.executeAsync(atBlock, mapper = pojo<String>()).result)
                return apiMethod.decodeOutput(runtime, encoded)
            }
            coroutineScope {
                val pallet = runtime.metadata.module("SubtensorModule")
                val id = BigInteger.valueOf(netuid.value.toLong())
                val stakeInfo = async { call("get_stake_info_for_coldkey", listOf(coldkey)) as List<*> }
                val availability = async {
                    bindBittensorExitAvailability(
                        call("get_stake_availability_for_coldkeys", listOf(listOf(coldkey), listOf(id))),
                        coldkey,
                        netuid.value
                    )
                }
                val owner = async { pallet.storage("Owner").query(hotkey, binding = { it?.let(::bindAccountIdKey) }) }
                val eligibility = async {
                    if (!netuid.isRoot()) {
                        require(pallet.storage("NetworksAdded").query(id, binding = ::bindBoolean)) { "Subnet no longer exists" }
                        require(pallet.storage("SubtokenEnabled").query(id, binding = ::bindBoolean)) { "Subnet trading is disabled" }
                    }
                }
                val coldkeySwap = async { pallet.storage("ColdkeySwapAnnouncements").query(coldkey, binding = { it }) }
                val safeMode = async { runtime.metadata.moduleOrNull("SafeMode")?.storageOrNull("EnteredUntil")?.query(binding = { it }) }
                val interval = async { pallet.storage("RootStakeUnlockInterval").query(binding = ::bindNumber) }
                val lastStake = async { pallet.storage("LastColdkeyHotkeyStakeBlock").query(coldkey, hotkey, binding = ::bindNumber) }
                val block = async { runtime.metadata.module("System").storage("Number").query(binding = ::bindNumber) }
                val factor = async { pallet.storage("NominatorMinRequiredStake").query(binding = ::bindNumber) }
                val minStake = pallet.numberConstant("InitialMinStake", runtime)

                val stake = stakeInfo.await()
                    .map { it.castToStruct() }
                    .filter { bindNumber(it["netuid"]) == id && bindAccountIdKey(it["hotkey"]).value.contentEquals(hotkey) }
                    .fold(BigInteger.ZERO) { sum, item -> sum + bindBittensorU64(item["stake"]) }
                require(stake.isPositive()) { "The selected staking position is empty" }
                val positionOwner = requireNotNull(owner.await()) { "The position's hotkey no longer exists" }
                eligibility.await()
                require(coldkeySwap.await() == null) { "Coldkey swap is pending" }
                require(safeMode.await() == null) { "The chain is in safe mode" }
                val unlockInterval = interval.await()
                val remaining = if (netuid.isRoot()) (lastStake.await() + unlockInterval - block.await()).max(BigInteger.ZERO) else BigInteger.ZERO
                BittensorExitPreflight(
                    stake = stake,
                    availability = availability.await(),
                    rootHoldRemaining = remaining,
                    rootHoldEnabled = unlockInterval.isPositive(),
                    minStake = minStake,
                    minNominatorStake = minStake * factor.await() / BittensorProtocolUnits.NOMINATOR_FACTOR_DENOMINATOR,
                    isOwnHotkey = positionOwner.value.contentEquals(coldkey)
                )
            }
        }
    }
}
