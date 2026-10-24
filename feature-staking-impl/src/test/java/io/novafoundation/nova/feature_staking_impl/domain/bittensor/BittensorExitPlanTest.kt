package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitPlanValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitRemainderValidation
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.toPayload
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorExitPlanTest {

    private val primary = AccountIdKey(ByteArray(32) { 1 })

    private val secondary = AccountIdKey(ByteArray(32) { 2 })

    private fun number(value: Long) = BigInteger.valueOf(value)

    private fun member(stake: Long, collateral: Long = 0, own: Boolean = false, hold: Long = 0) =
        BittensorExitMember(number(stake), number(collateral), own, number(hold))

    private fun basis(first: BittensorExitMember = member(600), second: BittensorExitMember = member(400), available: Long = 1000) =
        BittensorExitPreflight(
            first.stake, BittensorExitAvailability(number(1000), number(1000 - available), number(available)),
            first.holdRemaining, false, number(10), number(100), first.isOwnHotkey,
            groupStakes = mapOf(primary to first.stake, secondary to second.stake),
            groupMembers = mapOf(primary to first, secondary to second), withdrawalFloor = number(10)
        )

    private fun plan(preflight: BittensorExitPreflight, amount: Long, selection: BittensorExitSelection = BittensorExitSelection.All): BittensorExitPlan {
        val group = preflight.exitGroup(primary)
        return requireNotNull(group.basis(selection).plan(number(amount), group.floors))
    }

    private fun maximum(preflight: BittensorExitPreflight, selection: BittensorExitSelection? = null): BigInteger {
        val group = preflight.exitGroup(primary)
        return group.maximum(group.resolve(selection))
    }

    private fun quote(plan: BittensorExitPlan) = BittensorExitQuote(
        BittensorNetuid.Root, plan.amount, false, plan.amount, plan.amount, BigInteger.ZERO, null,
        plan = plan, groupHotkeys = setOf(primary, secondary)
    )

    @Test
    fun `eight of ten exits smallest and partially removes largest`() {
        val plan = plan(basis(), 800)
        assertEquals(listOf(BittensorExitSlice(secondary, number(400), true), BittensorExitSlice(primary, number(400), false)), plan.slices)
        assertTrue(plan.isBatched)
        val restored = quote(plan).toPayload(BittensorExitPositionPayload(1, 0, "hotkey")).quote()
        assertEquals(plan, restored.plan)
        assertEquals(setOf(primary, secondary), restored.groupHotkeys)
    }

    @Test
    fun `max exits all and smaller request stays on primary`() {
        assertTrue(plan(basis(), 1000).slices.all { it.fullExit })
        assertTrue(plan(basis(), 1000).emptiesGroup(listOf(primary, secondary)))
        assertEquals(listOf(BittensorExitSlice(primary, number(300), false)), plan(basis(), 300).slices)
        assertEquals(listOf(BittensorExitSlice(primary, number(600), true)), plan(basis(), 600).slices)
    }

    @Test
    fun `collateral on secondary and aggregate cap constrain drawable sum`() {
        val basis = basis(second = member(400, collateral = 200), available = 750)
        assertEquals(number(750), maximum(basis))
        assertFalse(basis.exitGroup(primary).basis(BittensorExitSelection.All).canExitAll)
        val plan = plan(basis, 750)
        assertEquals(listOf(number(200), number(550)), plan.slices.map { it.amount })
        assertTrue(plan.slices.none { it.fullExit })
    }

    @Test
    fun `collateral dust below the floor adds nothing to max and is skipped by the walk`() {
        val basis = basis(first = member(600, collateral = 595))
        assertEquals(number(400), maximum(basis))
        assertEquals(listOf(BittensorExitSlice(secondary, number(300), false)), plan(basis, 300).slices)
    }

    @Test
    fun `held member is excluded without hiding free member`() {
        val basis = basis(first = member(600, hold = 20))
        assertEquals(number(400), maximum(basis))
        assertEquals(secondary, plan(basis, 300).slices.single().hotkey)
    }

    @Test
    fun `several validators default to all and a picked one bounds max by its own stake`() {
        val group = basis(available = 500).exitGroup(primary)
        assertEquals(BittensorExitSelection.All, group.resolve(null))
        assertEquals(number(500), maximum(basis(available = 500)))
        val picked = BittensorExitSelection.Validator(secondary)
        assertEquals(picked, group.resolve(picked))
        assertEquals(number(400), maximum(basis(available = 500), picked))
        assertEquals(number(300), maximum(basis(available = 300), picked))
        assertEquals(listOf(BittensorExitSlice(secondary, number(400), true)), plan(basis(), 400, picked).slices)
        assertFalse(plan(basis(), 400, picked).emptiesGroup(group.hotkeys))
    }

    @Test
    fun `held or undrawable validator falls back to the largest sellable one`() {
        val held = basis(second = member(400, hold = 20)).exitGroup(primary)
        assertEquals(BittensorExitSelection.All, held.resolve(null))
        assertEquals(BittensorExitSelection.Validator(primary), held.resolve(BittensorExitSelection.Validator(secondary)))
        val single = basis(first = member(600, collateral = 595), second = member(400, hold = 20)).exitGroup(primary)
        assertFalse(single.canSellAll)
        assertEquals(BittensorExitSelection.Validator(primary), single.resolve(null))
    }

    @Test
    fun `all validators covers at most sixteen drawable legs`() {
        val hotkeys = (1..18).map { index -> AccountIdKey(ByteArray(32) { index.toByte() }) }
        val members = hotkeys.mapIndexed { index, hotkey -> hotkey to member(1_000L - index) }.toMap()
        val preflight = basis().copy(
            availability = BittensorExitAvailability(number(20_000), BigInteger.ZERO, number(20_000)),
            groupStakes = members.mapValues { it.value.stake },
            groupMembers = members
        )
        val group = preflight.exitGroup(hotkeys.first())
        assertTrue(group.isLegCapped(BittensorExitSelection.All))
        assertFalse(group.isLegCapped(BittensorExitSelection.Validator(hotkeys.first())))
        assertEquals(hotkeys.take(BittensorExitGroup.MAX_LEGS), group.basis(BittensorExitSelection.All).positions.map { it.hotkey })
    }

    @Test
    fun `all validators moves the last slice off a remainder the network would sweep`() {
        val swept = basis().copy(sweepFloor = number(150))
        assertEquals(listOf(BittensorExitSlice(secondary, number(400), true), BittensorExitSlice(primary, number(450), false)), plan(swept, 900).slices)
        assertEquals(listOf(BittensorExitSlice(primary, number(300), false)), plan(swept, 300).slices)
        assertEquals(listOf(BittensorExitSlice(secondary, number(400), true), BittensorExitSlice(primary, number(100), false)), plan(swept, 500).slices)
    }

    @Test
    fun `changed partial balance or missing member invalidates reviewed plan`() = runBlocking {
        val basis = basis()
        val plan = plan(basis, 800)
        val payload = BittensorExitValidationPayload(basis, quote(plan), number(100), BigInteger.ONE, BigInteger.ONE, true)
        val validation = BittensorExitPlanValidation()
        assertTrue(validation.validate(payload) is ValidationStatus.Valid)
        val changed = basis.copy(groupMembers = basis.groupMembers + (primary to member(300)))
        assertTrue(validation.validate(payload.copy(preflight = changed)) is ValidationStatus.NotValid)
        assertTrue(validation.validate(payload.copy(preflight = basis.copy(groupMembers = emptyMap()))) is ValidationStatus.NotValid)
    }

    @Test
    fun `dust is returned when availability covers its validator whatever the siblings hold and each owner is exempt`() = runBlocking {
        val plan = BittensorExitPlan(listOf(BittensorExitSlice(secondary, number(350), false)))
        val quote = quote(plan)
        val validation = BittensorExitRemainderValidation()
        val payload = BittensorExitValidationPayload(basis(), quote, number(100), BigInteger.ONE, BigInteger.ONE, true)
        listOf(basis(), basis(first = member(600, hold = 10)), basis(first = member(600, collateral = 300), available = 700)).forEach { preflight ->
            assertEquals(
                BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL,
                (validation.validate(payload.copy(preflight = preflight)) as ValidationStatus.NotValid).reason
            )
        }
        assertEquals(
            BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM,
            (validation.validate(payload.copy(preflight = basis(available = 399))) as ValidationStatus.NotValid).reason
        )
        assertTrue(validation.validate(payload.copy(preflight = basis(second = member(400, own = true)))) is ValidationStatus.Valid)
    }

    @Test
    fun `accepted returned dust moves on to the next slice in plan order`() = runBlocking {
        val plan = BittensorExitPlan(listOf(BittensorExitSlice(secondary, number(350), false), BittensorExitSlice(primary, number(550), false)))
        val validation = BittensorExitRemainderValidation()
        val payload = BittensorExitValidationPayload(basis(available = 500), quote(plan), number(100), BigInteger.ONE, BigInteger.ONE, true)
        assertEquals(BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL, (validation.validate(payload) as ValidationStatus.NotValid).reason)
        assertEquals(
            BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM,
            (validation.validate(payload.copy(remainderSweepAccepted = true)) as ValidationStatus.NotValid).reason
        )
    }

    @Test
    fun `floor uses ceiling fee adjustment and one percent ceiling margin`() {
        assertEquals(number(52), bittensorWithdrawalFloor(number(101), BittensorProtocolUnits.RAO_PER_TAO * number(2), BigInteger.ZERO))
        assertEquals(number(101), bittensorWithdrawalFloor(number(100), BittensorProtocolUnits.RAO_PER_TAO, BigInteger.ZERO))
        assertTrue(bittensorWithdrawalFloor(number(100), BittensorProtocolUnits.RAO_PER_TAO, number(33)) > number(101))
    }

    @Test
    fun `grouped partial freezes unselected members too`() = runBlocking {
        val untouched = AccountIdKey(ByteArray(32) { 3 })
        val added = AccountIdKey(ByteArray(32) { 4 })
        val original = basis().let { basis ->
            basis.copy(
                groupStakes = basis.groupStakes + (untouched to number(50)),
                groupMembers = basis.groupMembers + (untouched to member(50))
            )
        }
        val plan = BittensorExitPlan(
            listOf(
                BittensorExitSlice(secondary, number(400), true),
                BittensorExitSlice(primary, number(400), false)
            )
        )
        val reviewed = quote(plan).copy(groupHotkeys = original.groupStakes.keys)
        val restored = reviewed.toPayload(BittensorExitPositionPayload(1, 0, "hotkey")).quote()
        assertEquals(original.groupStakes.keys, restored.groupHotkeys)
        val payload = BittensorExitValidationPayload(original, restored, number(100), BigInteger.ONE, BigInteger.ONE, true)
        val validation = BittensorExitPlanValidation()
        assertTrue(validation.validate(payload) is ValidationStatus.Valid)
        val removed = original.copy(groupStakes = original.groupStakes - untouched, groupMembers = original.groupMembers - untouched)
        val expanded = original.copy(groupStakes = original.groupStakes + (added to number(10)), groupMembers = original.groupMembers + (added to member(10)))
        listOf(removed, expanded).forEach { changed ->
            val failure = validation.validate(payload.copy(preflight = changed)) as ValidationStatus.NotValid
            assertEquals(BittensorExitValidationFailure.FULL_POSITION_CHANGED, failure.reason)
        }
        // Identity is a set, as on iOS; map iteration order does not alter the frozen calls.
        val reordered = original.copy(groupStakes = original.groupStakes.entries.reversed().associate { it.toPair() })
        assertTrue(validation.validate(payload.copy(preflight = reordered)) is ValidationStatus.Valid)
        assertTrue(validation.validate(payload.copy(quote = restored.copy(groupHotkeys = emptySet()))) is ValidationStatus.NotValid)
    }

    @Test
    fun `single full slice still freezes group identity but single partial does not`() = runBlocking {
        val validation = BittensorExitPlanValidation()
        val fullSlice = BittensorExitPlan(listOf(BittensorExitSlice(secondary, number(400), true)))
        val original = BittensorExitValidationPayload(basis(), quote(fullSlice), number(100), BigInteger.ONE, BigInteger.ONE, true)
        val changed = basis().copy(groupStakes = mapOf(secondary to number(400)), groupMembers = mapOf(secondary to member(400)))
        assertTrue(validation.validate(original.copy(preflight = changed)) is ValidationStatus.NotValid)
        val partial = BittensorExitPlan(listOf(BittensorExitSlice(secondary, number(200), false)))
        assertTrue(validation.validate(original.copy(preflight = changed, quote = quote(partial))) is ValidationStatus.Valid)
    }
}
