package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorRootValidatorSourceTest {

    private val chain = mock(Chain::class.java)

    private val backend = mock(BittensorValidatorBackend::class.java)

    private val logger = mock(io.novasama.substrate_sdk_android.wsrpc.logging.Logger::class.java)

    private val source = RemoteBittensorRootValidatorSource(backend, logger)

    private val first = validator(1)

    private val second = validator(2)

    @Test
    fun `details return validator names and already annualized percentages`() = runBlocking {
        val rows = listOf(
            BittensorRootValidatorRemote.Validator(
                first.address,
                "Nova",
                BittensorRootValidatorRemote.Yield(BittensorRootValidatorRemote.Window("7.25")),
                "100"
            ),
            BittensorRootValidatorRemote.Validator(second.address, "Other", null, "100"),
            BittensorRootValidatorRemote.Validator("invalid", "Invalid", null, null)
        )
        `when`(backend.rootValidators()).thenReturn(BittensorRootValidatorRemote(rows, rootMeta()))

        val details = source.validatorDetails(chain)

        assertEquals(2, details.size)
        assertEquals("Nova", details[first.validator.hotkey]?.validator?.name)
        assertEquals(BigDecimal("7.25"), details[first.validator.hotkey]?.annualPercentageYield)
        assertNull(details[second.validator.hotkey]?.annualPercentageYield)
    }

    @Test
    fun `missing zero stake and negative yield remain unknown rather than zero`() {
        listOf(null, "0", "-1").forEach { stake ->
            val row = BittensorRootValidatorRemote.Validator(
                first.address,
                "Nova",
                BittensorRootValidatorRemote.Yield(BittensorRootValidatorRemote.Window("7")),
                stake
            )
            assertNull(mapRootValidatorDetails(chain, listOf(row))[first.validator.hotkey]?.annualPercentageYield)
        }
        val row = BittensorRootValidatorRemote.Validator(
            first.address,
            "Nova",
            BittensorRootValidatorRemote.Yield(BittensorRootValidatorRemote.Window("-1")),
            "10"
        )
        assertNull(mapRootValidatorDetails(chain, listOf(row))[first.validator.hotkey]?.annualPercentageYield)
    }

    private fun rootMeta(): BittensorRootValidatorRemote.Meta {
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", "2026-10-19T00:00:00Z")
        return BittensorRootValidatorRemote.Meta(BittensorRootValidatorRemote.Components(component, component))
    }

    private fun validator(seed: Byte): BittensorSubnetValidator {
        val validator = BittensorValidator(
            AccountIdKey(ByteArray(32) { seed }),
            "Validator $seed"
        )
        return BittensorSubnetValidator(
            validator,
            chain.addressOf(validator.hotkey),
            BigDecimal.ZERO,
            true
        )
    }
}
