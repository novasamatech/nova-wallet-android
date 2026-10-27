package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationKind
import io.novafoundation.nova.analytics.StakingValidationOutcome
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePreflightPayload
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetBuyQuote
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapSimulation
import io.novafoundation.nova.feature_staking_impl.R
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorStakeValidationUiTest {

    private val resources = mock(ResourceManager::class.java)

    private val format = BittensorStakeAlertFormat(
        nativeSymbol = "TAO",
        amount = { "$it rao" },
        requiredAmount = { "$it planks" },
        rate = { "rate ${it.limitPrice}" }
    )

    private var refreshes = 0

    private var rateAcceptances = 0

    private var resumptions = 0

    private var revalidations = 0

    private val actions = object : ValidationFlowActions<BittensorStakePreflightPayload> {
        override fun resumeFlow(modifyPayload: ((BittensorStakePreflightPayload) -> BittensorStakePreflightPayload)?) {
            resumptions += 1
        }

        override fun revalidate(modifyPayload: ((BittensorStakePreflightPayload) -> BittensorStakePreflightPayload)?) {
            revalidations += 1
        }
    }

    @Before
    fun setup() {
        `when`(resources.getString(anyInt())).thenAnswer { it.arguments[0].toString() }
    }

    @Test
    fun `stale quote is only refreshed after explicit user action and does not resume submission`() {
        val transformed = transform(BittensorStakeValidationFailure.QUOTE_EXPIRED) as TransformedFailure.Custom

        assertEquals(0, refreshes)
        transformed.dialogPayload.cancelAction!!.action()
        assertEquals(0, refreshes)
        transformed.dialogPayload.okAction!!.action()
        assertEquals(1, refreshes)
        assertEquals(0, rateAcceptances)
        assertEquals(0, resumptions)
        assertEquals(0, revalidations)
    }

    @Test
    fun `market move shows the old and new rate and confirm takes the new rate`() {
        `when`(resources.getString(R.string.bittensor_rate_updated_message, "rate 1", "rate 2")).thenReturn("Old rate: rate 1.\nNew rate: rate 2")
        val outcomes = mutableListOf<StakingValidationOutcome>()
        val failure = BittensorStakeValidationFailure.SlippageExceeded(quote(BigInteger.ONE), quote(BigInteger.TWO))

        val transformed = transform(failure, DefaultFailureLevel.WARNING) { outcomes += it } as TransformedFailure.Custom

        assertEquals(R.string.swap_rate_was_updated_failure_title.toString(), transformed.dialogPayload.title)
        assertEquals("Old rate: rate 1.\nNew rate: rate 2", transformed.dialogPayload.message)
        assertEquals(R.string.common_confirm.toString(), transformed.dialogPayload.okAction!!.title)
        transformed.dialogPayload.okAction!!.action()
        assertEquals(1, rateAcceptances)
        assertEquals(0, refreshes)
        assertEquals(listOf(StakingValidationOutcome.PROCEEDED), outcomes)
    }

    @Test
    fun `order too large for the tolerance only closes`() {
        val outcomes = mutableListOf<StakingValidationOutcome>()

        val failure = BittensorStakeValidationFailure.SELF_IMPACT

        val transformed = transform(failure) { outcomes += it } as TransformedFailure.Default

        assertEquals(R.string.bittensor_self_impact_title.toString(), transformed.titleAndMessage.first)
        assertEquals(R.string.bittensor_self_impact_message.toString(), transformed.titleAndMessage.second)
        assertEquals(listOf(StakingValidationOutcome.BLOCKED), outcomes)
    }

    @Test
    fun `missing quote shows the pool liquidity alert`() {
        val transformed = transform(BittensorStakeValidationFailure.QUOTE_UNAVAILABLE) as TransformedFailure.Default

        assertEquals(R.string.swap_not_enought_liquidity_failure.toString(), transformed.titleAndMessage.first)
        assertNull(transformed.titleAndMessage.second)
    }

    @Test
    fun `liquidity and order size report the iOS analytics kinds`() {
        assertEquals(StakingValidationKind.LIQUIDITY, BittensorStakeValidationFailure.QUOTE_UNAVAILABLE.analyticsKind())
        assertEquals(StakingValidationKind.ORDER_TOO_LARGE, BittensorStakeValidationFailure.SELF_IMPACT.analyticsKind())
        assertEquals(
            StakingValidationKind.RATE_UPDATED,
            BittensorStakeValidationFailure.SlippageExceeded(quote(BigInteger.ONE), quote(BigInteger.TWO)).analyticsKind()
        )
    }

    @Test
    fun `inactive warning cancel stays put and continue repeats validations`() {
        val transformed = transform(BittensorStakeValidationFailure.INACTIVE_VALIDATOR, DefaultFailureLevel.WARNING) as TransformedFailure.Custom

        transformed.dialogPayload.cancelAction!!.action()
        assertEquals(0, revalidations)
        transformed.dialogPayload.okAction!!.action()
        assertEquals(1, revalidations)
        assertEquals(0, resumptions)
        assertEquals(0, refreshes)
    }

    @Test
    fun `accepting price impact repeats validations instead of resuming past them`() {
        `when`(resources.getString(R.string.bittensor_price_impact_message, "1.66%")).thenReturn("This order moves the pool price by 1.66%.")
        val failure = BittensorStakeValidationFailure.HighPriceImpact(quote(BigInteger.TEN, amountIn = 302, spot = 300))

        val transformed = transform(failure, DefaultFailureLevel.WARNING) as TransformedFailure.Custom

        assertEquals(R.string.bittensor_price_impact_title.toString(), transformed.dialogPayload.title)
        assertEquals("This order moves the pool price by 1.66%.", transformed.dialogPayload.message)
        assertEquals(R.string.common_proceed.toString(), transformed.dialogPayload.okAction!!.title)
        transformed.dialogPayload.okAction!!.action()

        assertEquals(1, revalidations)
        assertEquals(0, resumptions)
        assertEquals(0, refreshes)
    }

    @Test
    fun `validation analytics reports only the chosen dialog outcome`() {
        val outcomes = mutableListOf<StakingValidationOutcome>()
        val failure = BittensorStakeValidationFailure.QUOTE_EXPIRED
        val transformed = transform(failure) { outcomes += it } as TransformedFailure.Custom
        assertEquals(emptyList<StakingValidationOutcome>(), outcomes)
        transformed.dialogPayload.cancelAction!!.action()
        assertEquals(listOf(StakingValidationOutcome.CANCELLED), outcomes)
        assertEquals(0, refreshes)
    }

    @Test
    fun `minimum stake shows the iOS amount too low copy with the required amount`() {
        `when`(resources.getString(R.string.bittensor_validation_minimum_stake, "122 planks"))
            .thenReturn("You can't stake less than the network minimum value (122 planks)")
        val outcomes = mutableListOf<StakingValidationOutcome>()
        val failure = BittensorStakeValidationFailure.BelowMinimumStake(122.toBigInteger())

        val transformed = transform(failure) { outcomes += it } as TransformedFailure.Default

        assertEquals(R.string.common_amount_low.toString(), transformed.titleAndMessage.first)
        assertEquals("You can't stake less than the network minimum value (122 planks)", transformed.titleAndMessage.second)
        assertEquals(listOf(StakingValidationOutcome.BLOCKED), outcomes)
    }

    @Test
    fun `fee reserve names the maximum stake and the reserve`() {
        `when`(resources.getString(R.string.bittensor_fee_reserve_title, "10000000 rao")).thenReturn("Keep 10000000 rao for fees")
        `when`(resources.getString(R.string.bittensor_fee_reserve_message, "123 rao", "10000000 rao")).thenReturn("Lower the amount to 123 rao or less.")
        val failure = BittensorStakeValidationFailure.FeeReserve(123.toBigInteger())

        val (title, message) = failure.titleAndMessage(resources, format)

        assertEquals("Keep 10000000 rao for fees", title)
        assertEquals("Lower the amount to 123 rao or less.", message)
    }

    @Test
    fun `network restrictions show their own titles`() {
        val restrictions = mapOf(
            BittensorStakeValidationFailure.HOTKEY_NOT_REGISTERED to (R.string.bittensor_validation_hotkey_title to R.string.bittensor_validation_hotkey),
            BittensorStakeValidationFailure.SUBNET_STAKING_DISABLED to
                (R.string.bittensor_validation_staking_disabled_title to R.string.bittensor_validation_staking_disabled),
            BittensorStakeValidationFailure.COLDKEY_SWAP_PENDING to
                (R.string.bittensor_validation_coldkey_swap_title to R.string.bittensor_validation_coldkey_swap),
            BittensorStakeValidationFailure.SAFE_MODE_ACTIVE to (R.string.bittensor_validation_safe_mode_title to R.string.bittensor_validation_safe_mode)
        )
        for ((reason, copy) in restrictions) {
            val (title, message) = reason.titleAndMessage(resources, format)

            assertEquals(copy.first.toString(), title)
            assertEquals(copy.second.toString(), message)
        }
    }

    @Test
    fun `balance failures keep the generic error title`() {
        val (title, message) = BittensorStakeValidationFailure.INSUFFICIENT_BALANCE.titleAndMessage(resources, format)

        assertEquals(R.string.common_error_general_title.toString(), title)
        assertEquals(R.string.common_error_not_enough_tokens.toString(), message)
    }

    @Test
    fun `proxied and delayed multisig refusals name the wallet type like iOS`() {
        `when`(resources.getString(R.string.bittensor_staking_wallet_unsupported_title, R.string.account_proxied.toString()))
            .thenReturn("Bittensor staking is not supported for Proxied wallets")
        `when`(resources.getString(R.string.bittensor_staking_wallet_unsupported_title, R.string.account_multisig.toString()))
            .thenReturn("Bittensor staking is not supported for Multisig wallets")
        `when`(resources.getString(R.string.bittensor_staking_wallet_unsupported_message, "TAO"))
            .thenReturn("To stake TAO, please switch to a different type of wallet.")
        val proxied = BittensorStakeValidationFailure.PROXIED_NOT_SUPPORTED.titleAndMessage(resources, format)
        val multisig = BittensorStakeValidationFailure.DELAYED_EXECUTION.titleAndMessage(resources, format)

        assertEquals("Bittensor staking is not supported for Proxied wallets", proxied.first)
        assertEquals("Bittensor staking is not supported for Multisig wallets", multisig.first)
        assertEquals("To stake TAO, please switch to a different type of wallet.", proxied.second)
        assertEquals("To stake TAO, please switch to a different type of wallet.", multisig.second)
    }

    private fun transform(
        failure: BittensorStakeValidationFailure,
        level: DefaultFailureLevel = DefaultFailureLevel.ERROR,
        onOutcome: (StakingValidationOutcome) -> Unit = {}
    ): TransformedFailure = bittensorStakeValidationFailureUi(
        ValidationStatus.NotValid(level, failure),
        actions,
        resources,
        format,
        onOutcome = onOutcome,
        acceptRate = { rateAcceptances += 1 }
    ) { refreshes += 1 }

    private fun quote(limitPrice: BigInteger, amountIn: Long = 1_000_000_000, spot: Long = 1_000_000_000): BittensorSubnetBuyQuote {
        val subnet = BittensorNetuid.Subnet(1)
        val unit = BigInteger.TEN.pow(9)
        val simulation = BittensorSwapSimulation(
            subnet,
            BittensorSwapDirection.TAO_TO_ALPHA,
            BigInteger.valueOf(amountIn),
            unit,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO
        )
        return BittensorSubnetBuyQuote(subnet, unit, BigInteger.ZERO, simulation, limitPrice, BigDecimal.ZERO, BigInteger.valueOf(spot), 0L)
    }
}
