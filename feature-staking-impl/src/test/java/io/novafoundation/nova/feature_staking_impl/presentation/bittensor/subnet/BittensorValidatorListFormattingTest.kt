package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.presentation.ellipsizeAddress
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorValidatorListFormattingTest {

    private val address = "5H3xdf9fnsySkhEjyWFnwndAii2Dj9LrtDh2oDtSzej2ZXvt"

    @Test
    fun `address fallback in row and selection button uses shared middle shortening`() {
        assertEquals(address.ellipsizeAddress(), bittensorValidatorDisplayName(row(address)))
        assertEquals(address.ellipsizeAddress(), bittensorValidatorDisplayName(row(" ")))
    }

    @Test
    fun `real validator identity is preserved`() {
        assertEquals("Nova Wallet", bittensorValidatorDisplayName(row("Nova Wallet")))
    }

    @Test
    fun `search matches name and full address case insensitively and trims query`() {
        val row = row("Nova Wallet")
        assertTrue(matchesBittensorValidator(row, "  NOVA  "))
        assertTrue(matchesBittensorValidator(row("Nóva Wallet"), "nova"))
        assertTrue(matchesBittensorValidator(row, "j9LrtDh"))
        assertTrue(matchesBittensorValidator(row, " "))
        assertFalse(matchesBittensorValidator(row, "unknown"))
    }

    private fun row(name: String): BittensorSubnetValidator {
        val validator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), name)
        return BittensorSubnetValidator(validator, address, BigDecimal.ZERO, selectable = true)
    }
}
