package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.Precision
import io.novafoundation.nova.common.validation.DefaultFailureLevel
import io.novafoundation.nova.common.validation.notValidOrNull
import io.novafoundation.nova.feature_account_api.data.extrinsic.SubmissionOrigin
import io.novafoundation.nova.feature_account_api.data.model.SubstrateFee
import io.novafoundation.nova.feature_staking_impl.di.validations.BittensorStakeValidationsModule
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorColdkeySwapValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorMinimumStakeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorOrderToleranceValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorPriceImpactValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorQuoteAvailabilityValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorQuoteFreshnessValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorQuoteTermsValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorRegisteredValidatorValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorSafeModeValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorSlippageValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorStakeFeeReserveValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorSubnetStakingEnabledValidation
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorStakeValidationsTest {

    private val asset = mock(Asset::class.java)

    private val token = mock(Token::class.java)

    private val chainAsset = mock(Chain.Asset::class.java)

    private val feeOrigin = SubmissionOrigin.singleOrigin(ByteArray(32))

    private val fee = SubstrateFee(BigInteger.TEN, feeOrigin, chainAsset)

    private val module = BittensorStakeValidationsModule()

    private val system = module.provideStakeValidationSystem()

    private val snapshot = BittensorStakePreflight(
        hotkeyRegistered = true,
        subnetExists = true,
        stakingEnabled = true,
        coldKeySwapPending = false,
        safeModeActive = false,
        minimumStake = BigInteger.valueOf(100),
        currentBlock = BigInteger.valueOf(100),
        rootUnlockInterval = BigInteger.TEN,
        lastStakeBlock = BigInteger.valueOf(90)
    )

    private val payload = BittensorStakeValidationPayload(
        asset = asset,
        amount = BigDecimal("0.1"),
        fee = fee,
        preflight = snapshot,
        quote = null,
        netuid = BittensorNetuid.Root,
        priceImpactAccepted = false,
        checkedAtMillis = 15_000L
    )

    @Before
    fun setup() {
        `when`(asset.token).thenReturn(token)
        `when`(token.configuration).thenReturn(chainAsset)
        `when`(chainAsset.precision).thenReturn(Precision(9))
        `when`(chainAsset.chainId).thenReturn("bittensor")
        `when`(asset.transferable).thenReturn(BigDecimal.ONE)
        `when`(asset.transferableInPlanks).thenReturn(BigInteger.TEN.pow(9))
    }

    @Test
    fun `valid root operation passes the complete validation system`() = runBlocking {
        assertNull(system.validate(payload).getOrThrow().notValidOrNull())
    }

    @Test
    fun `insufficient balance is rejected by the shared balance validation`() = runBlocking {
        val failure = system.validate(payload.copy(amount = BigDecimal("1.1"))).getOrThrow().notValidOrNull()
        assertEquals(BittensorStakeValidationFailure.INSUFFICIENT_BALANCE, failure?.reason)
    }

    @Test
    fun `registered hotkey is required`() = runBlocking {
        val validation = BittensorRegisteredValidatorValidation()
        assertFailure(validation, payload.copy(preflight = snapshot.copy(hotkeyRegistered = false)), BittensorStakeValidationFailure.HOTKEY_NOT_REGISTERED)
    }

    @Test
    fun `both subnet existence and staking availability are required`() = runBlocking {
        val validation = BittensorSubnetStakingEnabledValidation()
        assertFailure(validation, payload.copy(preflight = snapshot.copy(subnetExists = false)), BittensorStakeValidationFailure.SUBNET_STAKING_DISABLED)
        assertFailure(validation, payload.copy(preflight = snapshot.copy(stakingEnabled = false)), BittensorStakeValidationFailure.SUBNET_STAKING_DISABLED)
    }

    @Test
    fun `coldkey announcement and safe mode independently block staking`() = runBlocking {
        val coldkeyValidation = BittensorColdkeySwapValidation()
        val safeModeValidation = BittensorSafeModeValidation()
        assertFailure(
            coldkeyValidation,
            payload.copy(preflight = snapshot.copy(coldKeySwapPending = true)),
            BittensorStakeValidationFailure.COLDKEY_SWAP_PENDING
        )
        assertFailure(safeModeValidation, payload.copy(preflight = snapshot.copy(safeModeActive = true)), BittensorStakeValidationFailure.SAFE_MODE_ACTIVE)
    }

    @Test
    fun `fee reserve accepts exact maximum and rejects one rao above`() = runBlocking {
        val validation = BittensorStakeFeeReserveValidation()
        assertNull(validation.validate(payload.copy(amount = BigDecimal("0.989999990"))).notValidOrNull())
        assertFailure(validation, payload.copy(amount = BigDecimal("0.989999991")), BittensorStakeValidationFailure.FEE_RESERVE)
    }

    @Test
    fun `minimum stake includes service fee and pool fee exactly once`() = runBlocking {
        val validation = BittensorMinimumStakeValidation()
        val quote = quote().copy(serviceFeeTaoInPlanks = BigInteger.TEN)
        val withPoolFee = quote.copy(simulation = quote.simulation.copy(taoFee = BigInteger.valueOf(20)))
        assertNull(validation.validate(payload.copy(amount = BigDecimal("0.000000130"), quote = withPoolFee)).notValidOrNull())
        assertFailure(
            validation,
            payload.copy(amount = BigDecimal("0.000000129"), quote = withPoolFee),
            BittensorStakeValidationFailure.BelowMinimumStake(BigInteger.valueOf(120))
        )
    }

    @Test
    fun `root minimum stake reports the bare network minimum`() = runBlocking {
        val validation = BittensorMinimumStakeValidation()
        assertNull(validation.validate(payload.copy(amount = BigDecimal("0.000000100"), quote = null)).notValidOrNull())
        assertFailure(
            validation,
            payload.copy(amount = BigDecimal("0.000000099"), quote = null),
            BittensorStakeValidationFailure.BelowMinimumStake(BigInteger.valueOf(100))
        )
    }

    @Test
    fun `subnet minimum stake is grossed up by the service fee and the reported amount passes`() = runBlocking {
        val validation = BittensorMinimumStakeValidation()
        val rate = BigDecimal("0.0085")
        val commission = BittensorSubnetCommission(rate)
        fun quoteFor(total: Long) = quote().let {
            it.copy(
                totalTaoInPlanks = BigInteger.valueOf(total),
                serviceFeeTaoInPlanks = commission.feeFor(BigInteger.valueOf(total)),
                serviceFeeRate = rate,
                simulation = it.simulation.copy(taoFee = BigInteger.valueOf(20))
            )
        }
        val failure = validation.validate(payload.copy(amount = BigDecimal("0.000000100"), quote = quoteFor(100))).notValidOrNull()?.reason
        val required = (failure as BittensorStakeValidationFailure.BelowMinimumStake).requiredAmountInPlanks
        assertEquals(BigInteger.valueOf(122), required)

        val atRequired = payload.copy(amount = BigDecimal(required, 9), quote = quoteFor(required.toLong()))
        assertNull(validation.validate(atRequired).notValidOrNull())
    }

    @Test
    fun `subnet quote is required but root has no quote`() = runBlocking {
        val validation = BittensorQuoteAvailabilityValidation()
        assertNull(validation.validate(payload).notValidOrNull())
        assertFailure(validation, payload.copy(netuid = BittensorNetuid.Subnet(1)), BittensorStakeValidationFailure.QUOTE_UNAVAILABLE)
    }

    @Test
    fun `quote expires after fifteen seconds including after warning confirmation`() = runBlocking {
        val validation = BittensorQuoteFreshnessValidation()
        val value = payload.copy(quote = quote())
        assertNull(validation.validate(value).notValidOrNull())
        assertFailure(validation, value.copy(checkedAtMillis = 15_001L, priceImpactAccepted = true), BittensorStakeValidationFailure.QUOTE_EXPIRED)
        assertFailure(validation, value.copy(checkedAtMillis = -1L), BittensorStakeValidationFailure.QUOTE_EXPIRED)
    }

    @Test
    fun `order that reaches its own limit is too large for the tolerance`() = runBlocking {
        val validation = BittensorOrderToleranceValidation()
        val quote = quote()
        assertEquals(BigInteger.TEN.pow(9), quote.postTradePrice())
        assertNull(validation.validate(payload.copy(quote = quote)).notValidOrNull())
        assertFailure(validation, payload.copy(quote = quote.copy(limitPrice = quote.postTradePrice())), BittensorStakeValidationFailure.SELF_IMPACT)
    }

    @Test
    fun `market move past the limit is a warning`() = runBlocking {
        val validation = BittensorSlippageValidation()
        val quote = quote()
        val moved = quote.copy(limitPrice = quote.postTradePrice())
        assertNull(validation.validate(payload.copy(quote = quote, latestQuote = quote)).notValidOrNull())

        val failure = validation.validate(payload.copy(quote = quote, latestQuote = moved)).notValidOrNull()

        assertEquals(BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED, failure?.reason)
        assertEquals(DefaultFailureLevel.WARNING, failure?.level)
    }

    @Test
    fun `unread market blocks a subnet stake`() = runBlocking {
        val validation = BittensorSlippageValidation()
        val failure = validation.validate(payload.copy(quote = quote(), latestQuote = null)).notValidOrNull()

        assertEquals(BittensorStakeValidationFailure.QUOTE_UNAVAILABLE, failure?.reason)
        assertEquals(DefaultFailureLevel.ERROR, failure?.level)
    }

    @Test
    fun `self impacting order is reported before the balance`() = runBlocking {
        val quote = quote()
        val value = payload.copy(netuid = quote.subnetId, quote = quote.copy(limitPrice = quote.postTradePrice()), amount = BigDecimal("1.1"))
        assertEquals(BittensorStakeValidationFailure.SELF_IMPACT, system.validate(value).getOrThrow().notValidOrNull()?.reason)
    }

    @Test
    fun `high price impact is a warning that can be acknowledged`() = runBlocking {
        val validation = BittensorPriceImpactValidation()
        val quote = quote()
        val highImpact = quote.copy(simulation = quote.simulation.copy(amountOut = BigInteger.valueOf(980_000_000)))
        val value = payload.copy(quote = highImpact)
        val failure = validation.validate(value).notValidOrNull()
        assertEquals(BittensorStakeValidationFailure.HIGH_PRICE_IMPACT, failure?.reason)
        assertEquals(DefaultFailureLevel.WARNING, failure?.level)
        assertNull(validation.validate(value.copy(priceImpactAccepted = true)).notValidOrNull())
    }

    @Test
    fun `blocking errors take precedence over price impact warning`() = runBlocking {
        val quote = quote()
        val highImpact = quote.copy(simulation = quote.simulation.copy(amountOut = BigInteger.valueOf(980_000_000)))
        val value = payload.copy(quote = highImpact, preflight = snapshot.copy(safeModeActive = true))
        assertEquals(BittensorStakeValidationFailure.SAFE_MODE_ACTIVE, system.validate(value).getOrThrow().notValidOrNull()?.reason)
    }

    @Test
    fun `missing subnet quote is reported before the balance`() = runBlocking {
        val value = payload.copy(netuid = BittensorNetuid.Subnet(1), amount = BigDecimal("1.1"))
        assertEquals(BittensorStakeValidationFailure.QUOTE_UNAVAILABLE, system.validate(value).getOrThrow().notValidOrNull()?.reason)
    }

    @Test
    fun `inactive validator warning comes before price impact warning`() = runBlocking {
        val quote = quote()
        val highImpact = quote.copy(simulation = quote.simulation.copy(amountOut = BigInteger.valueOf(980_000_000)))
        val value = payload.copy(netuid = quote.subnetId, quote = highImpact, latestQuote = highImpact, inactiveValidator = true)

        assertEquals(BittensorStakeValidationFailure.INACTIVE_VALIDATOR, system.validate(value).getOrThrow().notValidOrNull()?.reason)
        assertEquals(
            BittensorStakeValidationFailure.HIGH_PRICE_IMPACT,
            system.validate(value.copy(inactiveValidatorAccepted = true)).getOrThrow().notValidOrNull()?.reason
        )
    }

    @Test
    fun `market move warning comes after price impact warning`() = runBlocking {
        val quote = quote()
        val highImpact = quote.copy(simulation = quote.simulation.copy(amountOut = BigInteger.valueOf(980_000_000)))
        val moved = highImpact.copy(limitPrice = highImpact.postTradePrice())
        val value = payload.copy(netuid = quote.subnetId, quote = highImpact, latestQuote = moved)

        assertEquals(BittensorStakeValidationFailure.HIGH_PRICE_IMPACT, system.validate(value).getOrThrow().notValidOrNull()?.reason)
        assertEquals(
            BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED,
            system.validate(value.copy(priceImpactAccepted = true)).getOrThrow().notValidOrNull()?.reason
        )
    }

    @Test
    fun `standard positive amount validation is reused`() = runBlocking {
        assertEquals(
            BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT,
            system.validate(payload.copy(amount = BigDecimal.ZERO)).getOrThrow().notValidOrNull()?.reason
        )
    }

    @Test
    fun `exactly one percent price impact does not warn`() = runBlocking {
        val validation = BittensorPriceImpactValidation()
        val quote = quote()
        val exactBoundary = quote.copy(
            spotPrice = BigInteger.valueOf(300),
            simulation = quote.simulation.copy(amountIn = BigInteger.valueOf(301)),
        )
        assertEquals(BigInteger.valueOf(303), exactBoundary.postTradePrice())
        assertNull(validation.validate(payload.copy(quote = exactBoundary)).notValidOrNull())
        val aboveBoundary = exactBoundary.copy(simulation = exactBoundary.simulation.copy(amountIn = BigInteger.valueOf(302)))
        assertFailure(validation, payload.copy(quote = aboveBoundary), BittensorStakeValidationFailure.HIGH_PRICE_IMPACT)
    }

    @Test
    fun `root top up is allowed during the withdrawal hold`() = runBlocking {
        val topUp = payload.copy(
            preflight = snapshot.copy(lastStakeBlock = snapshot.currentBlock)
        )

        assertNull(system.validate(topUp).getOrThrow().notValidOrNull())
    }

    @Test
    fun `fresh market data does not loosen the reviewed price limit`() = runBlocking {
        val validation = BittensorSlippageValidation()
        val reviewedQuote = quote().copy(limitPrice = BigInteger.valueOf(1_010_000_000))
        val latestQuote = reviewedQuote.copy(
            limitPrice = BigInteger.valueOf(1_100_000_000),
            simulation = reviewedQuote.simulation.copy(amountOut = BigInteger.valueOf(980_000_000))
        )

        assertFailure(
            validation,
            payload.copy(quote = reviewedQuote, latestQuote = latestQuote),
            BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED
        )
    }

    @Test
    fun `market validation uses the stricter current limit too`() = runBlocking {
        val validation = BittensorSlippageValidation()
        val reviewedQuote = quote()
        val latestQuote = reviewedQuote.copy(limitPrice = reviewedQuote.postTradePrice())

        assertFailure(
            validation,
            payload.copy(quote = reviewedQuote, latestQuote = latestQuote),
            BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED
        )
    }

    @Test
    fun `freshness is checked on the latest market snapshot`() = runBlocking {
        val validation = BittensorQuoteFreshnessValidation()
        val reviewedQuote = quote()
        val latestQuote = reviewedQuote.copy(capturedAtMillis = 20_000L)
        val value = payload.copy(quote = reviewedQuote, latestQuote = latestQuote, checkedAtMillis = 30_000L)

        assertNull(validation.validate(value).notValidOrNull())
        assertFailure(validation, value.copy(checkedAtMillis = 35_001L), BittensorStakeValidationFailure.QUOTE_EXPIRED)
    }

    @Test
    fun `changed service fee requires reviewing new terms`() = runBlocking {
        val validation = BittensorQuoteTermsValidation()
        val reviewedQuote = quote()
        val latestQuote = reviewedQuote.copy(serviceFeeTaoInPlanks = BigInteger.ONE)

        assertFailure(
            validation,
            payload.copy(quote = reviewedQuote, latestQuote = latestQuote),
            BittensorStakeValidationFailure.QUOTE_EXPIRED
        )
    }

    @Test
    fun `price impact warning uses the current market rather than the old quote`() = runBlocking {
        val validation = BittensorPriceImpactValidation()
        val reviewedQuote = quote()
        val latestQuote = reviewedQuote.copy(simulation = reviewedQuote.simulation.copy(amountOut = BigInteger.valueOf(980_000_000)))

        assertFailure(
            validation,
            payload.copy(quote = reviewedQuote, latestQuote = latestQuote),
            BittensorStakeValidationFailure.HIGH_PRICE_IMPACT
        )
    }

    @Test
    fun `buy post trade price rounds each division upward`() {
        val quote = quote()
        val rounded = quote.copy(
            spotPrice = BigInteger.valueOf(3),
            simulation = quote.simulation.copy(amountIn = BigInteger.TEN, amountOut = BigInteger.valueOf(3_000_000_000L))
        )
        assertEquals(BigInteger.valueOf(6), rounded.postTradePrice())
    }

    private suspend fun assertFailure(validation: BittensorStakeValidation, value: BittensorStakeValidationPayload, reason: BittensorStakeValidationFailure) {
        assertEquals(reason, validation.validate(value).notValidOrNull()?.reason)
    }

    private fun quote(): BittensorSubnetBuyQuote {
        val subnet = BittensorNetuid.Subnet(1)
        val unit = BigInteger.TEN.pow(9)
        val simulation = BittensorSwapSimulation(
            subnet,
            BittensorSwapDirection.TAO_TO_ALPHA,
            unit,
            unit,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO,
            BigInteger.ZERO
        )
        return BittensorSubnetBuyQuote(subnet, unit, BigInteger.ZERO, simulation, unit * BigInteger.TWO, BigDecimal.ZERO, unit, 0L)
    }
}
