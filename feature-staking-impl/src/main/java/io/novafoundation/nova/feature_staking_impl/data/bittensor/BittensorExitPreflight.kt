package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.extensions.toHexString
import java.math.BigInteger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitMember
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPlan
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitPlanMember
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.planBittensorExit

/** Withdrawal floors within 1 % of each other: the Max shown from the older one is still accurate enough. */
internal fun bittensorFloorsClose(first: BigInteger, second: BigInteger): Boolean =
    (first - second).abs() * BigInteger.valueOf(100) <= first.max(second)

data class BittensorExitAvailability(val total: BigInteger, val locked: BigInteger, val available: BigInteger)

data class BittensorExitPreflight(
    val stake: BigInteger,
    val availability: BittensorExitAvailability,
    val rootHoldRemaining: BigInteger,
    val rootHoldEnabled: Boolean,
    val minStake: BigInteger,
    val minNominatorStake: BigInteger,
    val isOwnHotkey: Boolean,
    val subnetTradingEnabled: Boolean = true,
    val coldkeySwapPending: Boolean = false,
    val safeModeActive: Boolean = false,
    val groupStakes: Map<AccountIdKey, BigInteger> = emptyMap(),
    val groupRootHoldRemaining: BigInteger = rootHoldRemaining,
    val groupMembers: Map<AccountIdKey, BittensorExitMember> = emptyMap(),
    val withdrawalFloor: BigInteger = BigInteger.ZERO,
    val withdrawalPrice: BigInteger? = null,
    val swapFeeRate: BigInteger = BigInteger.ZERO,
    val rootHoldInterval: BigInteger = BigInteger.ZERO
) {

    val totalStake: BigInteger
        get() = if (groupStakes.isEmpty()) stake else groupStakes.values.fold(BigInteger.ZERO, BigInteger::add)

    val orderedHotkeys: List<AccountIdKey>
        get() = groupStakes.entries
            .sortedWith(
                compareByDescending<Map.Entry<AccountIdKey, BigInteger>> { it.value }
                    .thenBy { it.key.value.toHexString() }
            )
            .map { it.key }

    val canExitAll: Boolean
        get() = availability.available >= totalStake && groupMembers.values.all { it.free == it.stake }

    val maximumAmount: BigInteger
        get() = if (groupMembers.isEmpty()) {
            totalStake.min(availability.available)
        } else {
            groupMembers.values.sumOf { it.drawable(withdrawalFloor) }.min(availability.available)
        }

    /**
     * Equality key for background refreshes: ignores per-block price drift and hold countdowns (only whether a
     * hotkey is held matters before the fresh validation read), so unchanged chain state does not re-quote.
     */
    fun refreshIdentity(): BittensorExitPreflight = copy(
        rootHoldRemaining = rootHoldRemaining.signum().toBigInteger(),
        groupRootHoldRemaining = groupRootHoldRemaining.signum().toBigInteger(),
        groupMembers = groupMembers.mapValues { (_, member) -> member.copy(holdRemaining = member.holdRemaining.signum().toBigInteger()) },
        withdrawalFloor = BigInteger.ZERO,
        withdrawalPrice = null
    )

    fun plan(amount: BigInteger, primary: AccountIdKey): BittensorExitPlan {
        val hotkeys = orderedHotkeys.ifEmpty { listOf(primary) }
        val members = hotkeys.map { hotkey ->
            BittensorExitPlanMember(
                hotkey,
                groupMembers[hotkey] ?: BittensorExitMember(
                    groupStakes[hotkey] ?: stake,
                    BigInteger.ZERO,
                    isOwnHotkey,
                    rootHoldRemaining
                )
            )
        }
        return planBittensorExit(members, amount, availability.available, withdrawalFloor)
    }
}
