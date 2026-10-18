package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.network.runtime.binding.bindAccountIdKey
import io.novafoundation.nova.common.data.network.runtime.binding.bindBoolean
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.numberConstant
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
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

    val owner = async {
        pallet.storage("Owner")
            .query(
                hotkey,
                binding = { it?.let(::bindAccountIdKey) }
            )
    }

    val eligibility = async {
        netuid.isRoot() || pallet.storage("NetworksAdded")
            .query(id, binding = ::bindBoolean) &&
            pallet.storage("SubtokenEnabled")
                .query(id, binding = ::bindBoolean)
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
            .query(binding = ::bindNumber)
    }

    val lastStake = async {
        pallet.storage("LastColdkeyHotkeyStakeBlock")
            .query(coldkey, hotkey, binding = ::bindNumber)
    }

    val block = async {
        runtime.metadata.module("System")
            .storage("Number")
            .query(binding = ::bindNumber)
    }

    val factor = async {
        pallet.storage("NominatorMinRequiredStake")
            .query(binding = ::bindNumber)
    }

    val minStake = pallet.numberConstant("InitialMinStake", runtime)

    val stake = stakeInfo.await().map { it.castToStruct() }
        .filter {
            bindNumber(it["netuid"]) == id && bindAccountIdKey(it["hotkey"]).value.contentEquals(hotkey)
        }
        .fold(BigInteger.ZERO) { sum, item -> sum + bindBittensorU64(item["stake"]) }

    val positionOwner = requireNotNull(owner.await()) { "The position's hotkey no longer exists" }
    val unlockInterval = interval.await()

    val remaining = if (netuid.isRoot()) {
        (lastStake.await() + unlockInterval - block.await())
            .max(BigInteger.ZERO)
    } else {
        BigInteger.ZERO
    }
    BittensorExitPreflight(
        stake = stake,
        availability = availability.await(),
        rootHoldRemaining = remaining,
        rootHoldEnabled = unlockInterval.isPositive(),
        minStake = minStake,
        minNominatorStake = minStake * factor.await() / BittensorProtocolUnits.NOMINATOR_FACTOR_DENOMINATOR,
        isOwnHotkey = positionOwner.value.contentEquals(coldkey),
        subnetTradingEnabled = eligibility.await(),
        coldkeySwapPending = coldkeySwap.await() != null,
        safeModeActive = safeMode.await() != null
    )
}
