package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitDustRemainder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitRemainderValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitQuoteValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitSelfImpactValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.dustRemainder
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorExitRemainderTest {

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val validation = BittensorExitRemainderValidation()

    // Finney on 2026-10-21: InitialMinStake = 2_000_000 rao, NominatorMinRequiredStake = 10_000_000 (per million).
    private val initialMinStake = rao(2_000_000)

    private val nominatorMinimum = bittensorNominatorMinStake(initialMinStake, rao(10_000_000))

    @Test
    fun `nominator minimum is a per million factor of the initial min stake as in subtensor`() {
        assertEquals(rao(20_000_000), nominatorMinimum)
        assertEquals(BigInteger.ZERO, bittensorNominatorMinStake(initialMinStake, BigInteger.ZERO))
        // subtensor doc example: 0.001 TAO default and factor 100_000_000 give 0.1 TAO.
        assertEquals(rao(100_000_000), bittensorNominatorMinStake(rao(1_000_000), rao(100_000_000)))
    }

    @Test
    fun `root partial leaving less than the nominator minimum warns and can proceed`() = runBlocking {
        // 0.01999 TAO staked, 0.005 TAO unstaked: 0.01499 TAO stays, below 0.02 TAO.
        val payload = payload(rootPreflight(stake = rao(19_990_000)), rootQuote(rao(5_000_000), stake = rao(19_990_000)))

        assertEquals(BittensorExitDustRemainder(rao(14_990_000), nominatorMinimum, returned = true), payload.dustRemainder())
        val status = validation.validate(payload) as ValidationStatus.NotValid
        assertEquals(BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL, status.reason)
        assertEquals(DefaultFailureLevel.WARNING, status.level)
        assertTrue(validation.validate(payload.copy(remainderSweepAccepted = true)) is ValidationStatus.Valid)
    }

    @Test
    fun `root partial keeping at least the nominator minimum is valid`() = runBlocking {
        val payload = payload(rootPreflight(stake = rao(30_000_000)), rootQuote(rao(10_000_000), stake = rao(30_000_000)))

        assertNull(payload.dustRemainder())
        assertTrue(validation.validate(payload) is ValidationStatus.Valid)
    }

    @Test
    fun `subnet remainder is valued in TAO at the limit price without a simulation`() = runBlocking {
        // 0.29745 alpha, sell 0.1: 0.19745 alpha stays at 0.067 TAO per alpha = 0.01322915 TAO, below 0.02 TAO.
        val stake = rao(297_450_000)
        val price = rao(67_000_000)
        val payload = payload(subnetPreflight(stake), subnetQuote(rao(100_000_000), stake, price))

        assertEquals(BittensorExitDustRemainder(rao(13_229_150), nominatorMinimum, returned = true), payload.dustRemainder())
        assertEquals(BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL, (validation.validate(payload) as ValidationStatus.NotValid).reason)

        // The same alpha remainder is enough at a higher price: 0.19745 × 0.2 = 0.03949 TAO.
        val pricier = payload(subnetPreflight(stake), subnetQuote(rao(100_000_000), stake, rao(200_000_000)))
        assertNull(pricier.dustRemainder())
        assertTrue(validation.validate(pricier) is ValidationStatus.Valid)
    }

    @Test
    fun `subnet remainder is valued at the pool price the sale leaves`() = runBlocking {
        // Selling 0.1 alpha at 0.11 spot for 0.0106 TAO leaves the pool at 0.10214545 TAO per alpha: 0.19745 alpha keeps 0.02016861 TAO.
        val stake = rao(297_450_000)
        val reviewed = subnetQuote(rao(100_000_000), stake, rao(67_000_000))
        val latest = reviewed.copy(estimatedTaoOut = rao(10_600_000), spotPrice = rao(110_000_000), simulatedInput = rao(100_000_000))
        val kept = payload(subnetPreflight(stake), reviewed).copy(latestQuote = latest)

        assertNull(kept.dustRemainder())
        assertTrue(validation.validate(kept) is ValidationStatus.Valid)

        // 0.0105 TAO out leaves 0.10022727 TAO per alpha: 0.01978987 TAO stays, below 0.02 TAO.
        val swept = kept.copy(latestQuote = latest.copy(estimatedTaoOut = rao(10_500_000)))
        assertEquals(BittensorExitDustRemainder(rao(19_789_874), nominatorMinimum, returned = true), swept.dustRemainder())
    }

    @Test
    fun `locked dust remainder cannot be accepted`() = runBlocking {
        val stake = rao(19_990_000)
        val locked = rootPreflight(stake).copy(availability = BittensorExitAvailability(stake, rao(1), stake - rao(1)))
        val payload = payload(locked, rootQuote(rao(5_000_000), stake)).copy(remainderSweepAccepted = true)

        val status = validation.validate(payload) as ValidationStatus.NotValid
        assertEquals(BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM, status.reason)
        assertEquals(DefaultFailureLevel.ERROR, status.level)
    }

    @Test
    fun `own hotkey and full exit never leave a nominator remainder`() = runBlocking {
        val stake = rao(19_990_000)
        val own = rootPreflight(stake).let { it.copy(isOwnHotkey = true, groupMembers = it.groupMembers.mapValues { (_, m) -> m.copy(isOwnHotkey = true) }) }
        assertNull(payload(own, rootQuote(rao(5_000_000), stake)).dustRemainder())

        val full = payload(rootPreflight(stake), rootQuote(stake, stake))
        assertNull(full.dustRemainder())
        assertTrue(validation.validate(full) is ValidationStatus.Valid)
    }

    @Test
    fun `amount above the drawable stake has no plan`() {
        val stake = rao(19_990_000)
        val group = rootPreflight(stake).exitGroup(hotkey)
        assertNull(group.basis(group.resolve(null)).plan(stake + BigInteger.ONE, group.floors))
    }

    @Test
    fun `grouped sale blocks a locked remainder after acknowledging a lower price`() = runBlocking {
        val otherHotkey = AccountIdKey(ByteArray(32) { 2 })
        val stakes = linkedMapOf(hotkey to rao(1_000_000_000), otherHotkey to rao(500_000_000))
        val available = rao(1_495_000_000)
        val basisPositions = stakes.map { (key, stake) -> BittensorExitBasisPosition(key, stake, BigInteger.ZERO, false, false) }
        val basis = BittensorExitBasis(stakes.values.sumOf { it }, available, basisPositions)
        val originalLimit = rao(995_000_000)
        val floors = BittensorExitFloors(
            bittensorWithdrawalFloor(initialMinStake, originalLimit, BigInteger.ZERO),
            bittensorWithdrawalFloor(nominatorMinimum, originalLimit, BigInteger.ZERO)
        )
        val plan = requireNotNull(basis.plan(available, floors))
        val original = groupedPayload(stakes, available, plan, originalLimit)
        // Review keeps the plan when the user acknowledges a lower rate. Its former safe remainder is now dust.
        val latest = original.quote.copy(
            limitPrice = rao(945_000_000),
            spotPrice = rao(951_000_000),
            simulatedInput = plan.amount,
            estimatedTaoOut = plan.amount * rao(950_500_000) / BittensorProtocolUnits.RAO_PER_TAO,
            minimumTaoOut = plan.amount * rao(945_000_000) / BittensorProtocolUnits.RAO_PER_TAO
        )
        val accepted = original.copy(quote = latest, latestQuote = latest, acknowledgedLimit = latest.limitPrice, remainderSweepAccepted = true)
        val selfImpactValidation = BittensorExitSelfImpactValidation()
        val quoteValidation = BittensorExitQuoteValidation()

        assertTrue(selfImpactValidation.validate(accepted) is ValidationStatus.Valid)
        assertTrue(quoteValidation.validate(accepted) is ValidationStatus.Valid)
        val status = validation.validate(accepted) as ValidationStatus.NotValid
        assertEquals(BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM, status.reason)
        assertEquals(DefaultFailureLevel.ERROR, status.level)

        val unlocked = accepted.copy(
            preflight = accepted.preflight.copy(
                availability = BittensorExitAvailability(rao(1_500_000_000), BigInteger.ZERO, rao(1_500_000_000))
            )
        )
        assertTrue(validation.validate(unlocked) is ValidationStatus.Valid)
    }

    @Test
    fun `full leg spends its current stake before checking the next remainder`() = runBlocking {
        val otherHotkey = AccountIdKey(ByteArray(32) { 2 })
        val plan = BittensorExitPlan(
            listOf(
                BittensorExitSlice(otherHotkey, rao(500_000_000), fullExit = true),
                BittensorExitSlice(hotkey, rao(995_000_000), fullExit = false)
            )
        )
        // The first position grew within the allowed 1 percent drift after the plan was frozen.
        val stakes = linkedMapOf(hotkey to rao(1_000_000_000), otherHotkey to rao(504_000_000))
        val accepted = groupedPayload(stakes, rao(1_500_000_000), plan).copy(remainderSweepAccepted = true)

        assertEquals(BittensorExitDustRemainder(rao(5_000_000), nominatorMinimum, returned = false), accepted.dustRemainder())
        assertEquals(BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM, (validation.validate(accepted) as ValidationStatus.NotValid).reason)
    }

    @Test
    fun `returned dust also consumes availability before the next partial leg`() = runBlocking {
        val otherHotkey = AccountIdKey(ByteArray(32) { 2 })
        val plan = BittensorExitPlan(
            listOf(
                BittensorExitSlice(hotkey, rao(995_000_000), fullExit = false),
                BittensorExitSlice(otherHotkey, rao(990_000_000), fullExit = false)
            )
        )
        val stakes = linkedMapOf(hotkey to rao(1_000_000_000), otherHotkey to rao(1_000_000_000))
        val accepted = groupedPayload(stakes, rao(1_995_000_000), plan).copy(remainderSweepAccepted = true)

        assertEquals(BittensorExitDustRemainder(rao(10_000_000), nominatorMinimum, returned = false), accepted.dustRemainder())
        assertEquals(BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM, (validation.validate(accepted) as ValidationStatus.NotValid).reason)
    }

    private fun groupedPayload(
        stakes: Map<AccountIdKey, BigInteger>,
        available: BigInteger,
        plan: BittensorExitPlan,
        limitPrice: BigInteger? = null
    ): BittensorExitValidationPayload {
        val total = stakes.values.sumOf { it }
        val preflight = rootPreflight(stakes.getValue(hotkey)).copy(
            availability = BittensorExitAvailability(total, total - available, available),
            groupStakes = stakes,
            groupMembers = stakes.mapValues { (_, stake) -> BittensorExitMember(stake, BigInteger.ZERO, false, BigInteger.ZERO) }
        )
        val quote = if (limitPrice == null) rootQuote(plan.amount, total) else subnetQuote(plan.amount, total, limitPrice)
        return payload(preflight, quote.copy(plan = plan, groupHotkeys = stakes.keys))
    }

    private fun rao(value: Long) = BigInteger.valueOf(value)

    private fun rootPreflight(stake: BigInteger) = preflight(stake, BittensorProtocolUnits.RAO_PER_TAO)

    private fun subnetPreflight(stake: BigInteger) = preflight(stake, null)

    private fun preflight(stake: BigInteger, price: BigInteger?) = BittensorExitPreflight(
        stake = stake,
        availability = BittensorExitAvailability(stake, BigInteger.ZERO, stake),
        rootHoldRemaining = BigInteger.ZERO,
        rootHoldEnabled = false,
        minStake = initialMinStake,
        minNominatorStake = nominatorMinimum,
        isOwnHotkey = false,
        groupStakes = mapOf(hotkey to stake),
        groupMembers = mapOf(hotkey to BittensorExitMember(stake, BigInteger.ZERO, false, BigInteger.ZERO)),
        withdrawalPrice = price
    )

    private fun rootQuote(amount: BigInteger, stake: BigInteger) = BittensorExitQuote(
        netuid = BittensorNetuid.Root,
        amount = amount,
        fullExit = amount == stake,
        estimatedTaoOut = amount,
        minimumTaoOut = amount,
        serviceFee = BigInteger.ZERO,
        limitPrice = null,
        exitHotkeys = if (amount == stake) listOf(hotkey) else emptyList(),
        plan = BittensorExitPlan(listOf(BittensorExitSlice(hotkey, amount, amount == stake)))
    )

    private fun subnetQuote(amount: BigInteger, stake: BigInteger, limitPrice: BigInteger) = BittensorExitQuote(
        netuid = BittensorNetuid.Subnet(64),
        amount = amount,
        fullExit = false,
        estimatedTaoOut = amount * limitPrice / BittensorProtocolUnits.RAO_PER_TAO + BigInteger.ONE,
        minimumTaoOut = amount * limitPrice / BittensorProtocolUnits.RAO_PER_TAO,
        serviceFee = BigInteger.ZERO,
        limitPrice = limitPrice,
        plan = BittensorExitPlan(listOf(BittensorExitSlice(hotkey, amount, amount == stake)))
    )

    private fun payload(preflight: BittensorExitPreflight, quote: BittensorExitQuote) = BittensorExitValidationPayload(
        preflight = preflight,
        quote = quote,
        transferable = BittensorProtocolUnits.RAO_PER_TAO,
        fee = BigInteger.ONE,
        existentialDeposit = BigInteger.ONE,
        supportsFeeFromStake = true
    )
}
