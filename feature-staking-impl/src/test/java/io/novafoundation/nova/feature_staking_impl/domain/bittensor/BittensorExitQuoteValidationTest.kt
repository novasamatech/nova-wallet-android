package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitPriceImpactValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitQuoteValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitSelfImpactValidation
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorExitQuoteValidationTest {

    private val scale = BittensorProtocolUnits.RAO_PER_TAO

    private val quote = BittensorExitQuote(
        BittensorNetuid.Subnet(1), scale, false, scale, scale * BigInteger.valueOf(98) / BigInteger.valueOf(100),
        BigInteger.ONE, scale * BigInteger.valueOf(98) / BigInteger.valueOf(100),
        spotPrice = scale, simulatedInput = scale
    )

    private val preflight = BittensorExitPreflight(
        scale,
        BittensorExitAvailability(scale, BigInteger.ZERO, scale),
        BigInteger.ZERO,
        false,
        BigInteger.ONE,
        BigInteger.ONE,
        false
    )

    private val payload = BittensorExitValidationPayload(preflight, quote, scale, BigInteger.ONE, BigInteger.ONE, true, latestQuote = quote)

    private val validation = BittensorExitQuoteValidation()

    private val impactValidation = BittensorExitPriceImpactValidation()

    private val selfImpactValidation = BittensorExitSelfImpactValidation()

    @Test
    fun `latest quote inside reviewed limit passes without changing reviewed terms`() = runBlocking {
        assertTrue(validation.validate(payload) is ValidationStatus.Valid)
        assertEquals(quote, payload.quote)
    }

    @Test
    fun `post trade price crossing reviewed limit blocks even if average exceeds limit`() = runBlocking {
        val output = scale * BigInteger.valueOf(985) / BigInteger.valueOf(1000)
        val latest = quote.copy(estimatedTaoOut = output)
        assertTrue(output > requireNotNull(quote.limitPrice))
        assertEquals(
            BittensorExitValidationFailure.QUOTE_CHANGED,
            (validation.validate(payload.copy(latestQuote = latest)) as ValidationStatus.NotValid).reason
        )
        assertEquals(quote, payload.quote)
    }

    @Test
    fun `unavailable quote blocks signing`() = runBlocking {
        assertTrue(validation.validate(payload.copy(latestQuote = null)) is ValidationStatus.NotValid)
    }

    @Test
    fun `latest quote still filling at the reviewed limit passes with another service fee`() = runBlocking {
        assertTrue(validation.validate(payload.copy(latestQuote = quote.copy(serviceFee = BigInteger.TEN))) is ValidationStatus.Valid)
    }

    @Test
    fun `latest quote after a price rise must fill at its own tighter limit`() = runBlocking {
        val latest = quote.copy(
            estimatedTaoOut = scale * BigInteger.valueOf(103) / BigInteger.valueOf(100),
            limitPrice = scale * BigInteger.valueOf(1029) / BigInteger.valueOf(1000),
            spotPrice = scale * BigInteger.valueOf(105) / BigInteger.valueOf(100)
        )
        assertTrue(latest.isFillable(atLimit = requireNotNull(quote.limitPrice)))
        assertEquals(
            BittensorExitValidationFailure.QUOTE_CHANGED,
            (validation.validate(payload.copy(latestQuote = latest)) as ValidationStatus.NotValid).reason
        )
    }

    @Test
    fun `impact is the post trade move of the shown quote and requires explicit consent`() = runBlocking {
        val shown = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(994) / BigInteger.valueOf(1000))
        val changed = payload.copy(quote = shown)
        assertEquals(BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH, (impactValidation.validate(changed) as ValidationStatus.NotValid).reason)
        assertTrue(impactValidation.validate(changed.copy(priceImpactAccepted = true)) is ValidationStatus.Valid)
    }

    @Test
    fun `impact of the re-read quote is not judged`() = runBlocking {
        val latest = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(994) / BigInteger.valueOf(1000))
        assertTrue(impactValidation.validate(payload.copy(latestQuote = latest)) is ValidationStatus.Valid)
    }

    @Test
    fun `sale moving the pool price past its own limit is too large for the tolerance`() = runBlocking {
        val belowLimit = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(985) / BigInteger.valueOf(1000))
        val belowMinimum = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(97) / BigInteger.valueOf(100))
        assertTrue(selfImpactValidation.validate(payload) is ValidationStatus.Valid)
        for (unfillable in listOf(belowLimit, belowMinimum)) {
            assertEquals(
                BittensorExitValidationFailure.ORDER_TOO_LARGE,
                (selfImpactValidation.validate(payload.copy(quote = unfillable)) as ValidationStatus.NotValid).reason
            )
        }
    }

    @Test
    fun `latest quote on the review is checked against the acknowledged limit`() = runBlocking {
        val reviewed = payload.copy(acknowledgedLimit = scale * BigInteger.valueOf(99) / BigInteger.valueOf(100))
        val holding = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(995) / BigInteger.valueOf(1000))
        val moved = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(994) / BigInteger.valueOf(1000))
        assertTrue(validation.validate(reviewed.copy(latestQuote = holding)) is ValidationStatus.Valid)
        assertTrue(validation.validate(payload.copy(latestQuote = moved)) is ValidationStatus.Valid)
        assertEquals(
            BittensorExitValidationFailure.QUOTE_CHANGED,
            (validation.validate(reviewed.copy(latestQuote = moved)) as ValidationStatus.NotValid).reason
        )
    }

    @Test
    fun `self impact on the review is judged at the tighter of the shown and acknowledged limit`() = runBlocking {
        val shown = payload.copy(quote = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(995) / BigInteger.valueOf(1000)))
        val acknowledged = shown.copy(acknowledgedLimit = scale * BigInteger.valueOf(995) / BigInteger.valueOf(1000))
        assertTrue(selfImpactValidation.validate(shown) is ValidationStatus.Valid)
        assertEquals(
            BittensorExitValidationFailure.ORDER_TOO_LARGE,
            (selfImpactValidation.validate(acknowledged) as ValidationStatus.NotValid).reason
        )
    }
}
