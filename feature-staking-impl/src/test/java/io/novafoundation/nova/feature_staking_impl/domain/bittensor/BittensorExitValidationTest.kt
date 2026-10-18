package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorExitValidationsModule
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BittensorExitValidationTest {

    private val validationsModule = BittensorExitValidationsModule()

    private val validationSystem = validationsModule.provideExitValidationSystem()

    private fun validateBittensorExit(preflight: BittensorExitPreflight, quote: BittensorExitQuote) = runBlocking {
        val payload = validationPayload(preflight, quote)
        require(
            validationSystem.validate(payload)
                .getOrThrow() is ValidationStatus.Valid
        )
    }

    private fun validationPayload(
        preflight: BittensorExitPreflight = preflight(),
        quote: BittensorExitQuote = quote()
    ) = BittensorExitValidationPayload(
        preflight,
        quote,
        stake,
        BigInteger.ONE,
        BigInteger.ONE,
        true
    )

    private fun assertFailure(payload: BittensorExitValidationPayload, expected: BittensorExitValidationFailure) = runBlocking {
        val result = validationSystem.validate(payload)
            .getOrThrow() as ValidationStatus.NotValid
        assertEquals(expected, result.reason)
    }

    @Test
    fun `fee cannot come from subnet stake`() {
        val payload = validationPayload(quote = quote(netuid = BittensorNetuid.Subnet(1)))
            .copy(transferable = BigInteger.ZERO)
        assertFailure(payload, BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE)
    }

    @Test
    fun `fee source must match review`() {
        val payload = validationPayload()
            .copy(expectedFeeFromStake = true)
        assertFailure(payload, BittensorExitValidationFailure.FEE_SOURCE_CHANGED)
    }

    @Test
    fun `root fee cannot use stake with hold enabled`() {
        val payload = validationPayload()
            .copy(transferable = BigInteger.ZERO, supportsFeeFromStake = false)
        assertFailure(payload, BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE)
    }

    @Test
    fun `root fee must leave minimum stake`() {
        val payload = validationPayload(quote = quote(stake - precision))
            .copy(transferable = BigInteger.ZERO)
        assertFailure(payload, BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM)
    }

    @Test
    fun `coldkey swap blocks exit`() {
        assertFailure(
            validationPayload(
                preflight()
                    .copy(coldkeySwapPending = true)
            ),
            BittensorExitValidationFailure.COLDKEY_SWAP_PENDING
        )
    }

    @Test
    fun `safe mode blocks exit`() {
        assertFailure(
            validationPayload(
                preflight()
                    .copy(safeModeActive = true)
            ),
            BittensorExitValidationFailure.SAFE_MODE_ACTIVE
        )
    }

    @Test
    fun `disabled subnet blocks exit`() {
        assertFailure(
            validationPayload(
                preflight()
                    .copy(subnetTradingEnabled = false)
            ),
            BittensorExitValidationFailure.SUBNET_TRADING_DISABLED
        )
    }

    private fun expectIllegal(block: () -> Unit) {
        try { block() } catch (expected: IllegalArgumentException) { return }
        fail("Expected an IllegalArgumentException")
    }

    private val precision = BigInteger.TEN.pow(9)

    private val stake = precision * BigInteger.TEN
    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val beneficiary = AccountIdKey(ByteArray(32) { 2 })

    private fun preflight(
        available: BigInteger = stake,
        hold: BigInteger = BigInteger.ZERO,
        minimum: BigInteger = precision,
        own: Boolean = false
    ) = BittensorExitPreflight(
        stake,
        BittensorExitAvailability(stake, stake - available, available),
        hold,
        false,
        precision,
        minimum,
        own
    )

    private fun quote(
        amount: BigInteger = precision * BigInteger.valueOf(5),
        full: Boolean = false,
        netuid: BittensorNetuid = BittensorNetuid.Root,
        limit: BigInteger? = if (netuid.isRoot()) null else precision
    ) = BittensorExitQuote(
        netuid,
        amount,
        full,
        amount,
        amount,
        BigInteger.ZERO,
        limit
    )

    @Test fun `partial root exit is valid`() {
        validateBittensorExit(preflight(), quote())
    }

    @Test fun `locked root cannot exit`() {
        expectIllegal { validateBittensorExit(preflight(hold = BigInteger.ONE), quote()) }
    }

    @Test fun `aggregate availability bounds withdrawal`() {
        expectIllegal { validateBittensorExit(preflight(available = precision), quote()) }
    }

    @Test fun `max cannot expand after confirmation`() {
        expectIllegal { validateBittensorExit(preflight(), quote(full = true)) }
    }

    @Test fun `full current position is valid`() {
        validateBittensorExit(preflight(), quote(stake, true))
    }

    @Test fun `full position with lock is rejected`() {
        expectIllegal { validateBittensorExit(preflight(available = stake - BigInteger.ONE), quote(stake, true)) }
    }

    @Test fun `partial withdrawal below minimum is rejected`() {
        expectIllegal { validateBittensorExit(preflight(), quote(BigInteger.ONE)) }
    }

    @Test fun `small full withdrawal bypasses partial minimum`() {
        val small = preflight()
            .copy(stake = BigInteger.ONE, availability = BittensorExitAvailability(BigInteger.ONE, BigInteger.ZERO, BigInteger.ONE))
        validateBittensorExit(small, quote(BigInteger.ONE, true))
    }

    @Test fun `nominator remainder must stay above minimum`() {
        expectIllegal { validateBittensorExit(preflight(minimum = stake), quote()) }
    }

    @Test fun `owner hotkey does not require nominator remainder`() {
        validateBittensorExit(preflight(minimum = stake, own = true), quote())
    }

    @Test fun `subnet remainder uses protective price`() {
        expectIllegal {
            validateBittensorExit(preflight(minimum = precision), quote(netuid = BittensorNetuid.Subnet(1), limit = precision / BigInteger.TEN))
        }
    }

    @Test fun `root cannot carry service fee or limit`() {
        expectIllegal {
            BittensorExitOperation(
                quote()
                    .copy(serviceFee = BigInteger.ONE),
                hotkey,
                beneficiary
            )
        }
        expectIllegal {
            BittensorExitOperation(
                quote()
                    .copy(limitPrice = precision),
                hotkey,
                beneficiary
            )
        }
    }

    @Test fun `sell minimum proceeds must cover service fee`() {
        expectIllegal {
            BittensorExitOperation(
                quote(netuid = BittensorNetuid.Subnet(1))
                    .copy(serviceFee = stake),
                hotkey,
                beneficiary
            )
        }
    }

    @Test fun `sell requires positive price and valid u64 amount`() {
        expectIllegal { BittensorExitOperation(quote(netuid = BittensorNetuid.Subnet(1), limit = BigInteger.ZERO), hotkey, beneficiary) }
        expectIllegal { BittensorExitOperation(quote(BigInteger.ONE.shiftLeft(64), netuid = BittensorNetuid.Subnet(1)), hotkey, beneficiary) }
    }
}
