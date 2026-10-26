package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSlippage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSwapDirection
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetReviewPayload
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSlippageSettingsTest {

    @Test
    fun `defaults presets range and warning thresholds match Bittensor`() {
        val preferences = preferences()
        val settings = BittensorSlippageSettings(preferences)
        val payload = settings.payload()
        val config = payload.config()

        assertEquals(BigDecimal("0.005"), settings.get())
        assertEquals(listOf(0.001, 0.005, 0.01, 0.03), payload.tips)
        assertEquals(0.0001, config.minAvailableSlippage.inFraction, 0.0)
        assertEquals(0.5, config.maxAvailableSlippage.inFraction, 0.0)
        assertEquals(0.001, config.smallSlippage.inFraction, 0.0)
        assertEquals(0.03, config.bigSlippage.inFraction, 0.0)
    }

    @Test
    fun `custom value and reset persist independently of swap settings`() {
        val values = mutableMapOf("swap.slippage" to "0.1")
        val preferences = preferences(values)
        val settings = BittensorSlippageSettings(preferences)
        settings.set(BigDecimal("0.0123"))
        val recreatedSettings = BittensorSlippageSettings(preferences)

        assertEquals(BigDecimal("0.0123"), recreatedSettings.get())
        assertEquals(0.0123, recreatedSettings.payload().selected, 0.0)
        assertEquals("0.1", values["swap.slippage"])
        recreatedSettings.set(BittensorSlippageSettings.DEFAULT)
        assertEquals(BittensorSlippageSettings.DEFAULT, settings.get())
    }

    @Test
    fun `inclusive limits are accepted and invalid settings rejected`() {
        val preferences = preferences()
        val settings = BittensorSlippageSettings(preferences)
        listOf("0.0001", "0.5").forEach {
            settings.set(BigDecimal(it))
            assertEquals(BigDecimal(it), settings.get())
        }
        listOf("0", "-0.1", "0.00009", "0.50001", "1").forEach {
            assertTrue(runCatching { settings.set(BigDecimal(it)) }.isFailure)
        }
        assertEquals(BigDecimal("0.5"), settings.get())
    }

    @Test
    fun `malformed or out of range stored settings fall back to default`() {
        listOf("bad", "NaN", "Infinity", "0", "0.6").forEach {
            val preferences = preferences(mutableMapOf("bittensor.slippage" to it))
            val settings = BittensorSlippageSettings(preferences)
            assertEquals(BittensorSlippageSettings.DEFAULT, settings.get())
        }
    }

    @Test
    fun `saved tolerance changes buy and sell protective limits`() {
        val preferences = preferences()
        val settings = BittensorSlippageSettings(preferences)
        settings.set(BigDecimal("0.03"))
        val slippage = BittensorSlippage(settings.get())
        val spot = BigInteger.valueOf(10000)

        assertEquals(BigInteger.valueOf(10300), slippage.limitPrice(spot, BittensorSwapDirection.TAO_TO_ALPHA))
        assertEquals(BigInteger.valueOf(9700), slippage.limitPrice(spot, BittensorSwapDirection.ALPHA_TO_TAO))
    }

    @Test
    fun `review payload freezes selected tolerance instead of rereading settings`() {
        val preferences = preferences()
        val settings = BittensorSlippageSettings(preferences)
        settings.set(BigDecimal("0.03"))
        val payload = BittensorSubnetReviewPayload(1, BigInteger.TEN, 42L, "validator", settings.get())
        settings.set(BigDecimal("0.01"))

        assertEquals(BigDecimal("0.03"), payload.slippage)
        assertEquals(BigDecimal("0.01"), settings.get())
    }

    private fun preferences(values: MutableMap<String, String> = mutableMapOf()): Preferences = Proxy.newProxyInstance(
        Preferences::class.java.classLoader,
        arrayOf(Preferences::class.java)
    ) { _, method, arguments ->
        when (method.name) {
            "getString" -> values[arguments[0]] ?: arguments.getOrNull(1)
            "putString" -> {
                values[arguments[0] as String] = arguments[1] as String
                null
            }
            else -> error("Unexpected preference access: ${method.name}")
        }
    } as Preferences
}
