package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorInitialValidatorTest {

    private val validator = BittensorValidator(AccountIdKey(ByteArray(32)), "Validator")

    @Test
    fun `discovery may open amount entry without a selected validator`() {
        assertNull(initialBittensorValidatorAddress(null, emptyList()))
    }

    @Test
    fun `initial selection prefers an eligible recommendation`() {
        val ordinary = row("ordinary")
        val recommended = row("recommended", recommended = true)
        assertEquals("recommended", initialBittensorValidatorAddress(null, listOf(ordinary, recommended)))
    }

    @Test
    fun `disabled recommendations are not selected`() {
        val disabled = row("disabled", selectable = false, recommended = true)
        assertEquals("ordinary", initialBittensorValidatorAddress(null, listOf(disabled, row("ordinary"))))
        assertNull(initialBittensorValidatorAddress(null, listOf(disabled)))
    }

    @Test
    fun `explicit validator is never replaced even when missing from refreshed directory`() {
        assertEquals("explicit", initialBittensorValidatorAddress("explicit", listOf(row("other", recommended = true))))
        assertEquals("explicit", initialBittensorValidatorAddress("explicit", emptyList()))
    }

    private fun row(address: String, selectable: Boolean = true, recommended: Boolean = false) =
        BittensorSubnetValidator(validator, address, BigDecimal.ZERO, selectable, recommended)
}
