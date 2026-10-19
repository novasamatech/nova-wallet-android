package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumberOrZero
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitMember
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorWithdrawalBounds
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRoot
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.metadata.module
import io.novasama.substrate_sdk_android.runtime.metadata.moduleOrNull
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageOrNull
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

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

    val eligibility = async {
        netuid.isRoot() || pallet.storage("NetworksAdded")
            .query(id) { it?.let(::bindBoolean) ?: false } &&
            pallet.storage("SubtokenEnabled")
                .query(id) { it?.let(::bindBoolean) ?: false }
    }

    val coldkeySwap = async {
        pallet.storage("ColdkeySwapAnnouncements")
            .query(
                coldkey,
                binding = { it }
            )
    }

    val safeMode = async {
        runtime.metadata.moduleOrNull("SafeMode")
            ?.storageOrNull("EnteredUntil")
            ?.query(binding = { it })
    }

    val interval = async {
        pallet.storage("RootStakeUnlockInterval")
            .query(binding = ::bindNumberOrZero)
    }

    val block = async {
        runtime.metadata.module("System")
            .storage("Number")
            .query(binding = ::bindNumber)
    }

    val factor = async {
        pallet.storage("NominatorMinRequiredStake")
            .query(binding = ::bindNumberOrZero)
    }

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

    val unlockInterval = interval.await()
    val members = groupStakes.entries.mapAsync { (member, memberStake) ->
        coroutineScope {
            val memberOwner = async {
                pallet.storage("Owner").query(member.value, binding = { it?.let(::bindAccountIdKey) })
            }
            val collateral = async {
                pallet.storage("MinerCollateral").query(
                    id,
                    member.value,
                    coldkey,
                    binding = { value -> value?.let { bindBittensorU64(it.castToStruct()["locked"]) } ?: BigInteger.ZERO }
                )
            }
            val hold = if (netuid.isRoot()) {
                val last = pallet.storage("LastColdkeyHotkeyStakeBlock")
                    .query(coldkey, member.value, binding = ::bindNumberOrZero)
                (last + unlockInterval - block.await()).max(BigInteger.ZERO)
            } else {
                BigInteger.ZERO
            }
            val owned = requireNotNull(memberOwner.await()) { "A group hotkey no longer exists" }
            member to BittensorExitMember(memberStake, collateral.await(), owned.value.contentEquals(coldkey), hold)
        }
    }.toMap()
    val primary = members.entries.firstOrNull { it.key.value.contentEquals(hotkey) }?.value
    val price = if (netuid.isRoot()) {
        BittensorProtocolUnits.RAO_PER_TAO
    } else {
        bindBittensorU64(
            callBittensorRuntimeApi(socket, blockHash, "SwapRuntimeApi", "current_alpha_price", listOf(id))
        )
    }
    val swapFeeRate = if (netuid.isRoot()) {
        BigInteger.ZERO
    } else {
        runtime.metadata.module("Swap")
            .storage("FeeRate")
            .query(id, binding = { it?.let(::bindNumber) ?: BittensorWithdrawalBounds.DEFAULT_SWAP_FEE_RATE })
    }
    BittensorExitPreflight(
        stake = stake,
        availability = availability.await(),
        rootHoldRemaining = primary?.holdRemaining ?: BigInteger.ZERO,
        rootHoldEnabled = unlockInterval.isPositive(),
        minStake = minStake,
        minNominatorStake = minStake * factor.await() / BittensorProtocolUnits.NOMINATOR_FACTOR_DENOMINATOR,
        isOwnHotkey = primary?.isOwnHotkey ?: false,
        subnetTradingEnabled = eligibility.await(),
        coldkeySwapPending = coldkeySwap.await() != null,
        safeModeActive = safeMode.await() != null,
        groupStakes = groupStakes,
        groupRootHoldRemaining = members.values.maxOfOrNull { it.holdRemaining } ?: BigInteger.ZERO,
        groupMembers = members,
        withdrawalPrice = price,
        swapFeeRate = swapFeeRate,
        rootHoldInterval = unlockInterval
    )
}
