package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorRootClaimPreview
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorClaimValidationsModule
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorClaimValidationTest {

    private val validationsModule = BittensorClaimValidationsModule()

    private val validationSystem = validationsModule.provideClaimValidationSystem()

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val preview = BittensorRootClaimPreview(
        hotkey,
        BigInteger.TEN,
        BigInteger.TEN,
        BigInteger.ONE
    )

    private val payload = BittensorClaimValidationPayload(
        preview,
        preview,
        BigInteger.TEN,
        BigInteger.ONE,
        BigInteger.ONE
    )

    @Test
    fun `unchanged rewards and sufficient fee balance are valid`() = runBlocking {
        assertTrue(
            validationSystem.validate(payload)
                .getOrThrow() is ValidationStatus.Valid
        )
    }

    @Test
    fun `unclaimable rewards are rejected`() {
        assertFailure(payload.copy(current = null), BittensorClaimValidationFailure.NOT_CLAIMABLE)
    }

    @Test
    fun `pending claim uses standard validation even when fee warning was accepted`() {
        assertFailure(payload.copy(claimPending = true, uneconomicClaimAccepted = true), BittensorClaimValidationFailure.CLAIM_PENDING)
    }

    @Test
    fun `decreased rewards require another review`() {
        assertFailure(payload.copy(current = preview.copy(redeemable = BigInteger.ONE)), BittensorClaimValidationFailure.REWARDS_DECREASED)
    }

    @Test
    fun `increased forfeiture requires another review`() {
        assertFailure(payload.copy(current = preview.copy(forfeitedEstimate = BigInteger.TEN)), BittensorClaimValidationFailure.FORFEITURE_INCREASED)
    }

    @Test
    fun `fee includes minimum account balance`() {
        assertFailure(payload.copy(transferable = BigInteger.ONE), BittensorClaimValidationFailure.INSUFFICIENT_FEE_BALANCE)
    }

    @Test
    fun `coldkey swap blocks claim`() {
        assertFailure(payload.copy(coldkeySwapPending = true), BittensorClaimValidationFailure.COLDKEY_SWAP_PENDING)
    }

    @Test
    fun `safe mode blocks claim`() {
        assertFailure(payload.copy(safeModeActive = true), BittensorClaimValidationFailure.SAFE_MODE_ACTIVE)
    }

    private fun assertFailure(payload: BittensorClaimValidationPayload, expected: BittensorClaimValidationFailure) = runBlocking {
        val result = validationSystem.validate(payload)
            .getOrThrow() as ValidationStatus.NotValid
        assertEquals(expected, result.reason)
    }

    @Test
    fun `one percent decrease and smaller do not require review`() = runBlocking {
        val reviewed = preview.copy(redeemable = BigInteger.valueOf(10_000))
        listOf(10_100L, 10_000L, 9_950L, 9_900L).forEach { current ->
            val input = payload.copy(reviewed = reviewed, current = reviewed.copy(redeemable = BigInteger.valueOf(current)))
            assertTrue(validationSystem.validate(input).getOrThrow() is ValidationStatus.Valid)
        }
    }

    @Test
    fun `one unit past one percent requires review without rounded percentage`() {
        val reviewed = preview.copy(redeemable = BigInteger.valueOf(10_000))
        assertFailure(
            payload.copy(reviewed = reviewed, current = reviewed.copy(redeemable = BigInteger.valueOf(9_899))),
            BittensorClaimValidationFailure.REWARDS_DECREASED
        )
    }

    @Test
    fun `forfeiture increase requires review even when rewards grew`() {
        assertFailure(
            payload.copy(current = preview.copy(redeemable = BigInteger.valueOf(20), forfeitedEstimate = BigInteger.TWO)),
            BittensorClaimValidationFailure.FORFEITURE_INCREASED
        )
    }

    @Test
    fun `equal or higher fee requires explicit acceptance`() {
        listOf(BigInteger.TEN, BigInteger.valueOf(11)).forEach { fee ->
            assertFailure(
                payload.copy(fee = fee, transferable = BigInteger.valueOf(100)),
                BittensorClaimValidationFailure.UNECONOMIC_CLAIM
            )
        }
    }

    @Test
    fun `accepted uneconomic claim still checks fee reserve and availability`() = runBlocking {
        val accepted = payload.copy(fee = BigInteger.TEN, transferable = BigInteger.valueOf(100), uneconomicClaimAccepted = true)
        assertTrue(validationSystem.validate(accepted).getOrThrow() is ValidationStatus.Valid)
        assertFailure(accepted.copy(transferable = BigInteger.ONE), BittensorClaimValidationFailure.INSUFFICIENT_FEE_BALANCE)
        assertFailure(accepted.copy(current = null), BittensorClaimValidationFailure.NOT_CLAIMABLE)
        assertFailure(accepted.copy(safeModeActive = true), BittensorClaimValidationFailure.SAFE_MODE_ACTIVE)
    }
}
