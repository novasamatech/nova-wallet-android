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

    private fun quote(plan: BittensorExitPlan) = BittensorExitQuote(
        BittensorNetuid.Root, plan.amount, false, plan.amount, plan.amount, BigInteger.ZERO, null,
        plan = plan, groupHotkeys = setOf(primary, secondary)
    )

    @Test
    fun `eight of ten exits smallest and partially removes largest`() {
        val plan = basis().plan(number(800), primary)
        assertEquals(listOf(BittensorExitSlice(secondary, number(400), true), BittensorExitSlice(primary, number(400), false)), plan.slices)
        assertTrue(plan.isBatched)
        val restored = quote(plan).toPayload(BittensorExitPositionPayload(1, 0, "hotkey")).quote()
        assertEquals(plan, restored.plan)
        assertEquals(setOf(primary, secondary), restored.groupHotkeys)
    }

    @Test
    fun `max exits all and smaller request stays on primary`() {
        assertTrue(basis().plan(number(1000), primary).slices.all { it.fullExit })
        assertEquals(listOf(BittensorExitSlice(primary, number(300), false)), basis().plan(number(300), primary).slices)
    }

    @Test
    fun `collateral on secondary and aggregate cap constrain drawable sum`() {
        val basis = basis(second = member(400, collateral = 200), available = 750)
        assertEquals(number(750), basis.maximumAmount)
        assertFalse(basis.canExitAll)
        val plan = basis.plan(number(750), primary)
        assertEquals(listOf(number(200), number(550)), plan.slices.map { it.amount })
        assertTrue(plan.slices.none { it.fullExit })
    }

    @Test
    fun `primary shortcut cannot consume collateral dust below floor`() {
        val basis = basis(first = member(600, collateral = 595))
        assertEquals(number(400), basis.maximumAmount)
        assertEquals(secondary, basis.plan(number(4), primary).slices.single().hotkey)
    }

    @Test
    fun `held member is excluded without hiding free member`() {
        val basis = basis(first = member(600, hold = 20))
        assertEquals(number(400), basis.maximumAmount)
        assertEquals(secondary, basis.plan(number(300), primary).slices.single().hotkey)
    }

    @Test
    fun `changed partial balance or missing member invalidates reviewed plan`() = runBlocking {
        val basis = basis()
        val plan = basis.plan(number(800), primary)
        val payload = BittensorExitValidationPayload(basis, quote(plan), number(100), BigInteger.ONE, BigInteger.ONE, true)
        val validation = BittensorExitPlanValidation()
        assertTrue(validation.validate(payload) is ValidationStatus.Valid)
        val changed = basis.copy(groupMembers = basis.groupMembers + (primary to member(300)))
        assertTrue(validation.validate(payload.copy(preflight = changed)) is ValidationStatus.NotValid)
        assertTrue(validation.validate(payload.copy(preflight = basis.copy(groupMembers = emptyMap()))) is ValidationStatus.NotValid)
    }

    @Test
    fun `available dust offers all locked dust errors and each owner is exempt`() = runBlocking {
        val plan = BittensorExitPlan(listOf(BittensorExitSlice(secondary, number(350), false)))
        val quote = quote(plan)
        val validation = BittensorExitRemainderValidation()
        val payload = BittensorExitValidationPayload(basis(), quote, number(100), BigInteger.ONE, BigInteger.ONE, true)
        assertEquals(BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL, (validation.validate(payload) as ValidationStatus.NotValid).reason)
        val locked = basis(second = member(400, collateral = 50), available = 950)
        assertEquals(
            BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM,
            (validation.validate(payload.copy(preflight = locked)) as ValidationStatus.NotValid).reason
        )
        assertTrue(validation.validate(payload.copy(preflight = basis(second = member(400, own = true)))) is ValidationStatus.Valid)
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
