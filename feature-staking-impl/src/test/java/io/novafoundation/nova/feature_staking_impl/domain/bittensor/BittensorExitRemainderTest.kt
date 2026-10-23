package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitDustRemainder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitRemainderValidation
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

        assertEquals(BittensorExitDustRemainder(rao(14_990_000), nominatorMinimum), payload.dustRemainder())
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
    fun `subnet remainder is valued in TAO at the limit price`() = runBlocking {
        // 0.29745 alpha, sell 0.1: 0.19745 alpha stays at 0.067 TAO per alpha = 0.01322915 TAO, below 0.02 TAO.
        val stake = rao(297_450_000)
        val price = rao(67_000_000)
        val payload = payload(subnetPreflight(stake), subnetQuote(rao(100_000_000), stake, price))

        assertEquals(BittensorExitDustRemainder(rao(13_229_150), nominatorMinimum), payload.dustRemainder())
        assertEquals(BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL, (validation.validate(payload) as ValidationStatus.NotValid).reason)

        // The same alpha remainder is enough at a higher price: 0.19745 × 0.2 = 0.03949 TAO.
        val pricier = payload(subnetPreflight(stake), subnetQuote(rao(100_000_000), stake, rao(200_000_000)))
        assertNull(pricier.dustRemainder())
        assertTrue(validation.validate(pricier) is ValidationStatus.Valid)
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
