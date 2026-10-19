package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class BittensorRootValidatorSourceTest {

    private val chain = mock(Chain::class.java)

    private val backend = mock(BittensorValidatorBackend::class.java)

    private val validators = mock(BittensorSubnetValidatorRepository::class.java)

    private val logger = mock(io.novasama.substrate_sdk_android.wsrpc.logging.Logger::class.java)

    private val source = RemoteBittensorRootValidatorSource(backend, validators, logger)

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
        `when`(backend.rootValidators()).thenReturn(BittensorRootValidatorRemote(rows, null))

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

    @Test
    fun `eligible primary root position takes priority over backend recommendations`() = runBlocking {
        `when`(validators.rootValidators(chain)).thenReturn(listOf(first, second))
        val positions = listOf(
            BittensorRootPosition(first.validator.hotkey, BigInteger.ONE),
            BittensorRootPosition(second.validator.hotkey, BigInteger.TEN)
        )

        assertEquals(second.validator, source.preferredValidator(chain, positions))
        verifyNoInteractions(backend)
    }

    @Test
    fun `new position uses the first eligible recommended hotkey rather than catalogue order`() = runBlocking {
        `when`(validators.rootValidators(chain)).thenReturn(listOf(first, second))
        `when`(backend.recommendedHotkeys(0)).thenReturn(listOf("invalid-address", second.address, first.address))

        assertEquals(second.validator, source.preferredValidator(chain, emptyList()))
    }

    @Test
    fun `ineligible existing validator is not silently reused`() = runBlocking {
        `when`(validators.rootValidators(chain)).thenReturn(listOf(first.copy(selectable = false), second))
        `when`(backend.recommendedHotkeys(0)).thenReturn(listOf(first.address, second.address))

        assertEquals(second.validator, source.preferredValidator(chain, listOf(BittensorRootPosition(first.validator.hotkey, BigInteger.TEN))))
    }

    @Test
    fun `no eligible recommendation leaves the validator unselected for manual choice`() = runBlocking {
        `when`(validators.rootValidators(chain)).thenReturn(listOf(first, second.copy(selectable = false)))
        `when`(backend.recommendedHotkeys(0)).thenReturn(listOf(second.address))

        assertNull(source.preferredValidator(chain, emptyList()))
    }

    @Test
    fun `backend failure cannot choose an arbitrary validator`() = runBlocking {
        `when`(validators.rootValidators(chain)).thenReturn(listOf(first, second))
        val failure = IllegalStateException("Recommendations unavailable")
        `when`(backend.recommendedHotkeys(0)).thenThrow(failure)

        assertSame(
            failure,
            runCatching { source.preferredValidator(chain, emptyList()) }
                .exceptionOrNull()
        )
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
