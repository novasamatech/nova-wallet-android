package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorExitValidationsModule
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.BittensorExitPositionPayload
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit.toPayload
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorGroupedExitTest {

    private val primary = AccountIdKey(ByteArray(32) { 1 })

    private val secondary = AccountIdKey(ByteArray(32) { 2 })

    private val validationsModule = BittensorExitValidationsModule()

    private val validations = validationsModule.provideExitValidationSystem()

    private val preflight = BittensorExitPreflight(
        stake = BigInteger.valueOf(600),
        availability = BittensorExitAvailability(BigInteger.valueOf(1000), BigInteger.ZERO, BigInteger.valueOf(1000)),
        rootHoldRemaining = BigInteger.ZERO,
        rootHoldEnabled = false,
        minStake = BigInteger.ONE,
        minNominatorStake = BigInteger.ONE,
        isOwnHotkey = false,
        groupStakes = mapOf(primary to BigInteger.valueOf(600), secondary to BigInteger.valueOf(400))
    )

    private val quote = BittensorExitQuote(
        netuid = BittensorNetuid.Root,
        amount = BigInteger.valueOf(1000),
        fullExit = true,
        estimatedTaoOut = BigInteger.valueOf(1000),
        minimumTaoOut = BigInteger.valueOf(1000),
        serviceFee = BigInteger.ZERO,
        limitPrice = null,
        exitHotkeys = listOf(primary, secondary)
    )

    private val payload = BittensorExitValidationPayload(
        preflight,
        quote,
        BigInteger.TEN,
        BigInteger.ONE,
        BigInteger.ONE,
        true
    )

    @Test
    fun `review payload preserves exact hotkeys and reviewed terms`() {
        val position = BittensorExitPositionPayload(10, 0, "validator-address")
        val restored = quote.toPayload(position).quote()
        assertEquals(quote, restored)
        assertEquals(listOf(primary, secondary), restored.exitHotkeys)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate validators cannot be signed`() {
        quote.copy(exitHotkeys = listOf(primary, primary))
    }

    @Test
    fun `max includes all validators when group is available`() {
        assertEquals(BigInteger.valueOf(1000), preflight.maximumAmount)
    }

    @Test
    fun `partly locked group caps max at aggregate availability`() {
        val available = preflight.availability.copy(available = BigInteger.valueOf(800))
        assertEquals(BigInteger.valueOf(800), preflight.copy(availability = available).maximumAmount)
    }

    @Test
    fun `partial cap cannot exceed available balance`() {
        val available = preflight.availability.copy(available = BigInteger.valueOf(200))
        assertEquals(BigInteger.valueOf(200), preflight.copy(availability = available).maximumAmount)
    }

    @Test
    fun `full group is valid`() = runBlocking {
        assertTrue(validations.validate(payload).getOrThrow() is ValidationStatus.Valid)
    }

    @Test
    fun `missing validator invalidates reviewed group`() = runBlocking {
        val changed = preflight.copy(groupStakes = mapOf(primary to BigInteger.valueOf(1000)))
        assertFailure(payload.copy(preflight = changed), BittensorExitValidationFailure.FULL_POSITION_CHANGED)
    }

    @Test
    fun `reordered validators invalidate reviewed group`() = runBlocking {
        val changed = preflight.copy(groupStakes = mapOf(primary to BigInteger.valueOf(400), secondary to BigInteger.valueOf(600)))
        assertFailure(payload.copy(preflight = changed), BittensorExitValidationFailure.FULL_POSITION_CHANGED)
    }

    @Test
    fun `one percent decrease is accepted but larger drift is rejected`() = runBlocking {
        val boundary = preflight.copy(groupStakes = mapOf(primary to BigInteger.valueOf(590), secondary to BigInteger.valueOf(400)))
        assertTrue(validations.validate(payload.copy(preflight = boundary)).getOrThrow() is ValidationStatus.Valid)
        val outside = boundary.copy(groupStakes = mapOf(primary to BigInteger.valueOf(589), secondary to BigInteger.valueOf(400)))
        assertFailure(payload.copy(preflight = outside), BittensorExitValidationFailure.FULL_POSITION_CHANGED)
    }

    @Test
    fun `hold on secondary validator blocks full exit`() = runBlocking {
        val locked = preflight.copy(groupRootHoldRemaining = BigInteger.ONE)
        assertFailure(payload.copy(preflight = locked), BittensorExitValidationFailure.ROOT_LOCKED)
    }

    @Test
    fun `group withdrawal cannot fund its batch fee from stake`() = runBlocking {
        assertFailure(payload.copy(transferable = BigInteger.ZERO), BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE)
    }

    private suspend fun assertFailure(payload: BittensorExitValidationPayload, expected: BittensorExitValidationFailure) {
        val result = validations.validate(payload).getOrThrow() as ValidationStatus.NotValid
        assertEquals(expected, result.reason)
    }
}
