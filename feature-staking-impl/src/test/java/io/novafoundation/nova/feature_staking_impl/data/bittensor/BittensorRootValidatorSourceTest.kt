package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectory
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorDirectoryStatus
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

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

    @Test
    fun `primary root position takes priority over the recommended preset without reading recommendations`() = runBlocking<Unit> {
        `when`(validators.chainValidator(chain, BittensorNetuid.Root, second.validator.hotkey)).thenReturn(second)
        val positions = listOf(
            BittensorRootPosition(first.validator.hotkey, BigInteger.ONE),
            BittensorRootPosition(second.validator.hotkey, BigInteger.TEN)
        )

        assertEquals(second.validator, source.preferredValidator(chain, positions))
        verify(validators, never()).directory(chain, BittensorNetuid.Root)
    }

    @Test
    fun `new position presets the recommended root validator with the highest root APY`() = runBlocking<Unit> {
        stubDirectory(first.copy(recommended = true), second.copy(recommended = true))
        `when`(backend.rootValidators()).thenReturn(BittensorRootValidatorRemote(listOf(rootRow(first, "5"), rootRow(second, "9")), rootMeta()))

        assertEquals(second.validator, source.preferredValidator(chain, emptyList()))
        verify(validators).validateRoot(chain, second.validator)
        verify(validators, never()).validateRoot(chain, first.validator)
    }

    @Test
    fun `existing root validator the chain cannot read falls back to the recommendation`() = runBlocking {
        stubDirectory(first.copy(recommended = true), second.copy(recommended = true))
        `when`(validators.chainValidator(chain, BittensorNetuid.Root, second.validator.hotkey)).thenThrow(IllegalStateException("offline"))

        assertEquals(first.validator, source.preferredValidator(chain, listOf(BittensorRootPosition(second.validator.hotkey, BigInteger.TEN))))
    }

    @Test
    fun `no selectable recommendation leaves the validator unselected for manual choice`() = runBlocking {
        stubDirectory(first, second.copy(selectable = false, recommended = true))

        assertNull(source.preferredValidator(chain, emptyList()))
    }

    @Test
    fun `directory failure cannot choose an arbitrary validator`() = runBlocking {
        stubDirectory(first.copy(recommended = true), failure = IllegalStateException("Recommendations unavailable"))

        assertEquals("Recommendations unavailable", runCatching { source.preferredValidator(chain, emptyList()) }.exceptionOrNull()?.message)
    }

    private suspend fun stubDirectory(vararg rows: BittensorSubnetValidator, failure: Throwable? = null) {
        val status = mock(BittensorValidatorDirectoryStatus::class.java)
        `when`(validators.directory(chain, BittensorNetuid.Root)).thenReturn(BittensorValidatorDirectory(rows.toList(), status, failure))
    }

    private fun rootRow(validator: BittensorSubnetValidator, apy: String) = BittensorRootValidatorRemote.Validator(
        validator.address,
        validator.validator.name,
        BittensorRootValidatorRemote.Yield(BittensorRootValidatorRemote.Window(apy)),
        "100"
    )

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
