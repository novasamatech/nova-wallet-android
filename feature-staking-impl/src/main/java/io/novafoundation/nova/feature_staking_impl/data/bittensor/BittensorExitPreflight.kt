package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.extensions.toHexString
import java.math.BigInteger
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitBasisPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitFloors
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitGroup
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitMember
import io.novasama.substrate_sdk_android.hash.isPositive

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
    val rootHoldInterval: BigInteger = BigInteger.ZERO,
    val sweepFloor: BigInteger = BigInteger.ZERO
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
        withdrawalPrice = null,
        sweepFloor = BigInteger.ZERO
    )

    /** The group in portfolio order; a snapshot without stake info stands for the [primary] position alone. */
    fun exitGroup(primary: AccountIdKey): BittensorExitGroup {
        val positions = orderedHotkeys.ifEmpty { listOf(primary) }.map { hotkey ->
            val member = groupMembers[hotkey] ?: BittensorExitMember(groupStakes[hotkey] ?: stake, BigInteger.ZERO, isOwnHotkey, rootHoldRemaining)
            BittensorExitBasisPosition(hotkey, member.stake, member.collateral, member.isOwnHotkey, member.holdRemaining.isPositive())
        }
        return BittensorExitGroup(positions, availability.available, BittensorExitFloors(withdrawalFloor, sweepFloor))
    }
}
