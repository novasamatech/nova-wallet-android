package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.data.network.rpc.QueryStorageAtRequest
import io.novafoundation.nova.common.data.network.rpc.QueryStorageAtResponse
import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitMember
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWithdrawalBounds
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.bittensorNominatorMinStake
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.fromHex
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.module.StorageEntry
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.executeAsync
import io.novasama.substrate_sdk_android.wsrpc.mappers.nonNull
import io.novasama.substrate_sdk_android.wsrpc.mappers.pojoList
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Two storage round trips regardless of the number of group hotkeys: every plain/global item in one
 * `state_queryStorageAt`, then every per-hotkey item once the coldkey's hotkeys are known. The runtime API
 * calls run concurrently with the first batch.
 */
internal suspend fun StorageQueryContext.loadBittensorExitSnapshot(
    coldkey: AccountId,
    hotkey: AccountId,
    netuid: BittensorNetuid,
    socket: SocketService,
    blockHash: String
): BittensorExitPreflight = coroutineScope {
    val pallet = runtime.metadata.module("SubtensorModule")
    val id = BigInteger.valueOf(netuid.value.toLong())

    val stakeInfo = async {
        callBittensorRuntimeApi(
            socket,
            blockHash,
            "StakeInfoRuntimeApi",
            "get_stake_info_for_coldkey",
            listOf(coldkey)
        ) as List<*>
    }

    val availability = async {
        bindBittensorExitAvailability(
            callBittensorRuntimeApi(
                socket,
                blockHash,
                "StakeInfoRuntimeApi",
                "get_stake_availability_for_coldkeys",
                listOf(listOf(coldkey), listOf(id))
            ),
            coldkey,
            netuid.value
        )
    }

    val price = async {
        if (netuid.isRoot()) {
            BittensorProtocolUnits.RAO_PER_TAO
        } else {
            bindBittensorU64(
                callBittensorRuntimeApi(socket, blockHash, "SwapRuntimeApi", "current_alpha_price", listOf(id))
            )
        }
    }

    val globals = BittensorStorageBatch(runtime)
    val networkAdded = globals.takeUnless { netuid.isRoot() }
        ?.add(pallet.storage("NetworksAdded"), id) { it?.let(::bindBoolean) ?: false }
    val subtokenEnabled = globals.takeUnless { netuid.isRoot() }
        ?.add(pallet.storage("SubtokenEnabled"), id) { it?.let(::bindBoolean) ?: false }
    val coldkeySwap = globals.add(pallet.storage("ColdkeySwapAnnouncements"), coldkey) { it }
    val safeMode = runtime.metadata.moduleOrNull("SafeMode")
        ?.storageOrNull("EnteredUntil")
        ?.let { globals.add(it) { value -> value } }
    val interval = globals.add(pallet.storage("RootStakeUnlockInterval"), binding = ::bindNumberOrZero)
    val block = globals.add(runtime.metadata.module("System").storage("Number"), binding = ::bindNumber)
    val factor = globals.add(pallet.storage("NominatorMinRequiredStake"), binding = ::bindNumberOrZero)
    val swapFeeRate = globals.takeUnless { netuid.isRoot() }?.add(runtime.metadata.module("Swap").storage("FeeRate"), id) {
        it?.let(::bindNumber) ?: BittensorWithdrawalBounds.DEFAULT_SWAP_FEE_RATE
    }
    val globalsLoaded = async { globals.execute(socket, blockHash) }

    val minStake = pallet.numberConstant("InitialMinStake", runtime)

    val groupStakes = stakeInfo.await()
        .map { it.castToStruct() }
        .filter { bindNumber(it["netuid"]) == id }
        .groupBy { bindAccountIdKey(it["hotkey"]) }
        .mapValues { (_, positions) -> positions.fold(BigInteger.ZERO) { sum, item -> sum + bindBittensorU64(item["stake"]) } }
        .filterValues { it.isPositive() }

    val stake = groupStakes.entries
        .firstOrNull { it.key.value.contentEquals(hotkey) }
        ?.value ?: BigInteger.ZERO

    val memberReads = BittensorStorageBatch(runtime)
    val memberItems = groupStakes.keys.associateWith { member ->
        BittensorExitMemberItems(
            owner = memberReads.add(pallet.storage("Owner"), member.value) { it?.let(::bindAccountIdKey) },
            collateral = memberReads.add(pallet.storage("MinerCollateral"), id, member.value, coldkey) { value ->
                value?.let { bindBittensorU64(it.castToStruct()["locked"]) } ?: BigInteger.ZERO
            },
            lastStakeBlock = memberReads.takeIf { netuid.isRoot() }
                ?.add(pallet.storage("LastColdkeyHotkeyStakeBlock"), coldkey, member.value, binding = ::bindNumberOrZero)
        )
    }
    memberReads.execute(socket, blockHash)
    globalsLoaded.await()

    val unlockInterval = interval.value
    val members = groupStakes.mapValues { (member, memberStake) ->
        val items = memberItems.getValue(member)
        val hold = items.lastStakeBlock?.let { last -> (last.value + unlockInterval - block.value).max(BigInteger.ZERO) } ?: BigInteger.ZERO
        val owned = requireNotNull(items.owner.value) { "A group hotkey no longer exists" }
        BittensorExitMember(memberStake, items.collateral.value, owned.value.contentEquals(coldkey), hold)
    }
    val primary = members.entries.firstOrNull { it.key.value.contentEquals(hotkey) }?.value
    BittensorExitPreflight(
        stake = stake,
        availability = availability.await(),
        rootHoldRemaining = primary?.holdRemaining ?: BigInteger.ZERO,
        rootHoldEnabled = unlockInterval.isPositive(),
        minStake = minStake,
        minNominatorStake = bittensorNominatorMinStake(minStake, factor.value),
        isOwnHotkey = primary?.isOwnHotkey ?: false,
        subnetTradingEnabled = netuid.isRoot() || requireNotNull(networkAdded).value && requireNotNull(subtokenEnabled).value,
        coldkeySwapPending = coldkeySwap.value != null,
        safeModeActive = safeMode?.value != null,
        groupStakes = groupStakes,
        groupRootHoldRemaining = members.values.maxOfOrNull { it.holdRemaining } ?: BigInteger.ZERO,
        groupMembers = members,
        withdrawalPrice = price.await(),
        swapFeeRate = swapFeeRate?.value ?: BigInteger.ZERO,
        rootHoldInterval = unlockInterval
    )
}

private class BittensorExitMemberItems(
    val owner: BittensorStorageBatch.Item<AccountIdKey?>,
    val collateral: BittensorStorageBatch.Item<BigInteger>,
    val lastStakeBlock: BittensorStorageBatch.Item<BigInteger>?
)

/** Collects storage items (plain or keyed) and reads them at one block in a single `state_queryStorageAt`. */
private class BittensorStorageBatch(private val runtime: RuntimeSnapshot) {

    inner class Item<V>(private val entry: StorageEntry, val key: String, private val binding: (Any?) -> V) {

        val value: V by lazy {
            val loaded = checkNotNull(values) { "Storage batch has not been executed" }
            binding(loaded[key]?.let { entry.type.value?.fromHex(runtime, it) })
        }
    }

    private val keys = mutableListOf<String>()

    private var values: Map<String, String?>? = null

    fun <V> add(entry: StorageEntry, vararg arguments: Any?, binding: (Any?) -> V): Item<V> {
        val key = entry.storageKey(runtime, *arguments)
        keys += key
        return Item(entry, key, binding)
    }

    suspend fun execute(socket: SocketService, blockHash: String) {
        val distinct = keys.distinct()
        values = if (distinct.isEmpty()) {
            emptyMap()
        } else {
            socket.executeAsync(QueryStorageAtRequest(distinct, blockHash), mapper = pojoList<QueryStorageAtResponse>().nonNull())
                .first()
                .changesAsMap()
        }
    }
}
