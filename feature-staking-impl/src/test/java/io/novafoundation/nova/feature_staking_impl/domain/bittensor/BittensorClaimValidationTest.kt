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
}
