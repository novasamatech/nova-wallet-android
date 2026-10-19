package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitPriceImpactValidation
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validations.BittensorExitQuoteValidation
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
    fun `unavailable quote and commission change block signing`() = runBlocking {
        assertTrue(validation.validate(payload.copy(latestQuote = null)) is ValidationStatus.NotValid)
        assertTrue(validation.validate(payload.copy(latestQuote = quote.copy(serviceFee = BigInteger.TEN))) is ValidationStatus.NotValid)
    }

    @Test
    fun `impact is post trade move not average and requires explicit consent`() = runBlocking {
        val latest = quote.copy(estimatedTaoOut = scale * BigInteger.valueOf(994) / BigInteger.valueOf(1000))
        val changed = payload.copy(latestQuote = latest)
        assertEquals(BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH, (impactValidation.validate(changed) as ValidationStatus.NotValid).reason)
        assertTrue(impactValidation.validate(changed.copy(priceImpactAccepted = true)) is ValidationStatus.Valid)
    }
}
