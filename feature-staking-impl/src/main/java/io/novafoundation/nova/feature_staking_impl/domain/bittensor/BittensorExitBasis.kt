package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger

/** One validator of the group, or every sellable validator. */
sealed interface BittensorExitSelection {

    data class Validator(val hotkey: AccountIdKey) : BittensorExitSelection

    data object All : BittensorExitSelection
}

/** The smallest slice worth withdrawing and the nominator remainder the network sweeps. */
data class BittensorExitFloors(val withdrawal: BigInteger, val sweep: BigInteger)

/** One position of the group; a root hold leaves nothing drawable until it ends. */
data class BittensorExitBasisPosition(
    val hotkey: AccountIdKey,
    val alpha: BigInteger,
    val locked: BigInteger,
    val isOwned: Boolean,
    val isHeld: Boolean
) {

    val free: BigInteger get() = (alpha - locked).max(BigInteger.ZERO)

    fun drawable(floor: BigInteger): BigInteger = when {
        isHeld -> BigInteger.ZERO
        !locked.isPositive() -> alpha
        free >= floor -> free
        else -> BigInteger.ZERO
    }

    fun leavesSweepFloor(slice: BigInteger, floors: BittensorExitFloors): Boolean {
        return locked.isPositive() || isOwned || (alpha - slice).max(BigInteger.ZERO) >= floors.sweep
    }
}

/** The positions one withdrawal may draw from, capped by the coldkey's availability. */
data class BittensorExitBasis(
    val total: BigInteger,
    val available: BigInteger,
    val positions: List<BittensorExitBasisPosition>
) {

    val canExitAll: Boolean
        get() = available >= total && positions.none { it.locked.isPositive() } && positions.sumOf { it.alpha } == total

    fun drawable(floor: BigInteger): BigInteger = positions.sumOf { it.drawable(floor) }

    fun max(floor: BigInteger): BigInteger = available.min(drawable(floor))

    /**
     * The primary takes the whole amount when it can; otherwise the smallest positions are drained first and the last
     * slice moves down to the nearest amount that leaves no remainder for the network to sweep, so the planned amount may
     * be smaller than the requested one. It never grows past the requested amount: the validations warn about the remainder instead.
     */
    fun plan(amount: BigInteger, floors: BittensorExitFloors): BittensorExitPlan? {
        val primary = positions.firstOrNull()
        if (!amount.isPositive() || amount > max(floors.withdrawal) || primary == null) return null
        if (!primary.locked.isPositive() && amount == primary.alpha) {
            return planOf(exits = listOf(BittensorExitSlice(primary.hotkey, amount, fullExit = true)))
        }
        if (positions.size == 1 || primaryCovers(amount, primary, floors)) {
            return planOf(partials = listOf(BittensorExitSlice(primary.hotkey, amount, fullExit = false)))
        }

        val exits = mutableListOf<BittensorExitSlice>()
        val partials = mutableListOf<BittensorExitSlice>()
        var needed = amount
        for (position in positions.asReversed()) {
            if (!needed.isPositive()) break
            val drawable = position.drawable(floors.withdrawal)
            if (!drawable.isPositive()) continue
            if (needed < drawable) return adjustedPlan(exits, partials, position, needed, floors)
            needed -= drawable
            if (position.locked.isPositive()) {
                partials += BittensorExitSlice(position.hotkey, drawable, fullExit = false)
            } else {
                exits += BittensorExitSlice(position.hotkey, position.alpha, fullExit = true)
            }
        }
        return planOf(exits, partials)
    }

    private fun primaryCovers(amount: BigInteger, primary: BittensorExitBasisPosition, floors: BittensorExitFloors): Boolean {
        return amount <= primary.free && amount < drawable(floors.withdrawal) && primary.leavesSweepFloor(amount, floors)
    }

    private fun adjustedPlan(
        exits: List<BittensorExitSlice>,
        partials: List<BittensorExitSlice>,
        position: BittensorExitBasisPosition,
        needed: BigInteger,
        floors: BittensorExitFloors
    ): BittensorExitPlan? {
        val drawn = (exits + partials).sumOf { it.amount }
        val drawable = position.drawable(floors.withdrawal)
        val max = max(floors.withdrawal)

        fun fits(slice: BigInteger): Boolean = if (!slice.isPositive() || slice >= drawable) {
            slice.isPositive() || drawn.isPositive()
        } else {
            slice >= floors.withdrawal && position.leavesSweepFloor(slice, floors)
        }

        val nearest = listOf(needed, BigInteger.ZERO, (position.free - floors.sweep).max(BigInteger.ZERO), drawable)
            .filter { it <= drawable && drawn + it <= max && fits(it) }
            .minWithOrNull(compareBy<BigInteger> { (it - needed).abs() }.thenBy { it })
            ?: return null
        // A larger slice would silently enlarge the requested amount, so the requested slice stays.
        val slice = nearest.min(needed)

        return if (slice.isPositive()) {
            planOf(exits, partials + BittensorExitSlice(position.hotkey, slice, fullExit = false))
        } else {
            planOf(exits, partials)
        }
    }

    private fun planOf(exits: List<BittensorExitSlice> = emptyList(), partials: List<BittensorExitSlice> = emptyList()): BittensorExitPlan? {
        return (exits + partials).takeIf { it.isNotEmpty() }?.let(::BittensorExitPlan)
    }
}

/**
 * A coldkey's positions on one netuid in portfolio order (largest stake first, then hotkey; the first is the
 * primary), with the floors of the current price.
 */
class BittensorExitGroup(
    val positions: List<BittensorExitBasisPosition>,
    private val availableStake: BigInteger,
    val floors: BittensorExitFloors
) {

    companion object {

        /** The most validators one "All validators" withdrawal covers. */
        const val MAX_LEGS = 16
    }

    init {
        require(positions.isNotEmpty())
    }

    val hotkeys: List<AccountIdKey> = positions.map { it.hotkey }

    val heldHotkeys: Set<AccountIdKey> = positions.filter { it.isHeld }.mapTo(mutableSetOf()) { it.hotkey }

    /** Held, or with nothing drawable above the withdrawal floor. */
    val unsellableHotkeys: Set<AccountIdKey> = positions.filterNot { it.drawable(floors.withdrawal).isPositive() }.mapTo(mutableSetOf()) { it.hotkey }

    val canSellAll: Boolean get() = positions.size > 1 && positions.any { it.hotkey !in unsellableHotkeys }

    /** Nothing chosen yet means all validators when the group has several and one can sell. */
    fun resolve(requested: BittensorExitSelection?): BittensorExitSelection {
        val sellable = hotkeys.filterNot { it in unsellableHotkeys }
        val fallback = BittensorExitSelection.Validator(sellable.firstOrNull() ?: hotkeys.first())
        return when (requested) {
            null, BittensorExitSelection.All -> if (canSellAll) BittensorExitSelection.All else fallback
            is BittensorExitSelection.Validator -> if (requested.hotkey in sellable) requested else fallback
        }
    }

    fun basis(selection: BittensorExitSelection): BittensorExitBasis {
        val members = when (selection) {
            is BittensorExitSelection.Validator -> positions.filter { it.hotkey == selection.hotkey }
            BittensorExitSelection.All -> capLegs(positions.filterNot { it.isHeld })
        }
        val total = members.sumOf { it.alpha }
        return BittensorExitBasis(total, total.min(availableStake), members)
    }

    fun maximum(selection: BittensorExitSelection): BigInteger = basis(selection).max(floors.withdrawal)

    /** The amount Max fills in: the planned amount at the maximum, which moves down off a remainder the network would sweep. */
    fun maximumFill(selection: BittensorExitSelection): BigInteger {
        val basis = basis(selection)
        val maximum = basis.max(floors.withdrawal)
        return basis.plan(maximum, floors)?.amount ?: maximum
    }

    fun isLegCapped(selection: BittensorExitSelection): Boolean {
        return selection == BittensorExitSelection.All && positions.count { it.hotkey !in unsellableHotkeys } > MAX_LEGS
    }

    /** The selected validator, else the largest sellable one, else the primary. */
    fun anchor(selection: BittensorExitSelection): AccountIdKey {
        return (selection as? BittensorExitSelection.Validator)?.hotkey
            ?: positions.firstOrNull { it.hotkey !in unsellableHotkeys }?.hotkey
            ?: hotkeys.first()
    }

    private fun capLegs(members: List<BittensorExitBasisPosition>): List<BittensorExitBasisPosition> {
        val undrawable = members.filterNot { it.drawable(floors.withdrawal).isPositive() }.mapTo(mutableSetOf()) { it.hotkey }
        val legs = members.filterNot { it.hotkey in undrawable }.take(MAX_LEGS).mapTo(mutableSetOf()) { it.hotkey }
        return members.filter { it.hotkey in legs || it.hotkey in undrawable }
    }
}

/** Only full exits, covering every validator of the group. */
fun BittensorExitPlan.emptiesGroup(groupHotkeys: Collection<AccountIdKey>): Boolean {
    return slices.all { it.fullExit } && slices.map { it.hotkey }.containsAll(groupHotkeys)
}
