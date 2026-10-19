package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorValidatorSnapshotTest {

    private val chain = mock(Chain::class.java)

    private val validator = BittensorValidator(AccountIdKey(ByteArray(32) { 1 }), "Validator")

    private val gates = BittensorValidatorGates(BigDecimal("0.2"), requirePermit = true, requireActive = true)

    private val snapshot = BittensorValidatorSnapshot(
        uids = mapOf(validator.hotkey to 0),
        commissionsInU16 = mapOf(validator.hotkey to BigInteger.ZERO),
        permits = listOf(true),
        lastUpdates = listOf(BigInteger.TEN),
        activeThreshold = BigInteger.TEN
    )

    @Test
    fun `subnet permit and activity gates are evaluated from the same snapshot`() {
        assertTrue(mapBittensorValidators(chain, BittensorNetuid.Subnet(1), listOf(validator), gates, snapshot).single().selectable)
        assertTrue(mapBittensorValidators(chain, BittensorNetuid.Subnet(1), listOf(validator), gates, snapshot.copy(permits = listOf(false))).isEmpty())
        val inactive = snapshot.copy(lastUpdates = listOf(BigInteger.ONE))
        assertFalse(mapBittensorValidators(chain, BittensorNetuid.Subnet(1), listOf(validator), gates, inactive).single().selectable)
    }

    @Test
    fun `root ignores subnet-only gates but requires registration and an allowed take`() {
        val root = snapshot.copy(permits = emptyList(), lastUpdates = emptyList())
        assertTrue(mapBittensorValidators(chain, BittensorNetuid.Root, listOf(validator), gates, root).single().selectable)
        assertTrue(mapBittensorValidators(chain, BittensorNetuid.Root, listOf(validator), gates, root.copy(uids = emptyMap())).isEmpty())
        val excessiveCommission = root.copy(commissionsInU16 = mapOf(validator.hotkey to BigInteger.valueOf(65535)))
        assertTrue(mapBittensorValidators(chain, BittensorNetuid.Root, listOf(validator), gates, excessiveCommission).isEmpty())
    }

    @Test
    fun `subnet permit and activity are optional when backend flags are disabled`() {
        val disabledGates = gates.copy(requirePermit = false, requireActive = false)
        val withoutPermit = snapshot.copy(permits = listOf(false))
        val inactive = snapshot.copy(lastUpdates = listOf(BigInteger.ZERO))
        val subnet = BittensorNetuid.Subnet(1)

        assertTrue(mapBittensorValidators(chain, subnet, listOf(validator), disabledGates, withoutPermit).single().selectable)
        assertTrue(mapBittensorValidators(chain, subnet, listOf(validator), disabledGates, inactive).single().selectable)
        assertTrue(mapBittensorValidators(chain, subnet, listOf(validator), disabledGates, snapshot.copy(uids = emptyMap())).isEmpty())
        val excessiveTake = snapshot.copy(commissionsInU16 = mapOf(validator.hotkey to BigInteger.valueOf(65535)))
        assertTrue(mapBittensorValidators(chain, subnet, listOf(validator), disabledGates, excessiveTake).isEmpty())
    }

    @Test
    fun `raw delegate commission maps to a decimal rate`() {
        val unrestrictedGates = BittensorValidatorGates(BigDecimal.ONE, requirePermit = false, requireActive = false)
        val expectedRates = mapOf(0 to "0", 13107 to "0.2", 65535 to "1")

        expectedRates.forEach { (rawCommission, expectedRate) ->
            val commissionSnapshot = snapshot.copy(commissionsInU16 = mapOf(validator.hotkey to rawCommission.toBigInteger()))
            val mapped = mapBittensorValidators(chain, BittensorNetuid.Root, listOf(validator), unrestrictedGates, commissionSnapshot)
                .single()

            assertEquals(0, BigDecimal(expectedRate).compareTo(mapped.commissionRate))
        }
    }
}
