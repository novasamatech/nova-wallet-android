package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.validatorCommissionRateFromU16
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
    fun `only an inactive subnet validator cannot take a new stake`() {
        assertTrue(map(BittensorNetuid.Subnet(1), snapshot).canSelect)
        val noPermit = map(BittensorNetuid.Subnet(1), snapshot.copy(permits = listOf(false)))
        assertFalse(noPermit.canSelect)
        assertTrue(noPermit.inactive)
        assertTrue(noPermit.canAddToExistingPosition)
        assertFalse(map(BittensorNetuid.Subnet(1), snapshot.copy(lastUpdates = listOf(BigInteger.ONE))).canSelect)
        val unregistered = map(BittensorNetuid.Subnet(1), snapshot.copy(uids = emptyMap()))
        assertTrue(unregistered.inactive)
        assertTrue(unregistered.canAddToExistingPosition)
    }

    @Test
    fun `validator above the take limit stays listed and selectable but is not recommended`() {
        val excessiveTake = snapshot.copy(commissionsInU16 = mapOf(validator.hotkey to BigInteger.valueOf(65535)))

        listOf(BittensorNetuid.Subnet(1), BittensorNetuid.Root).forEach { netuid ->
            assertTrue(map(netuid, excessiveTake).canSelect)
            assertTrue(snapshot.passesBittensorRecommendationGates(netuid, validator.hotkey, gates))
            assertFalse(excessiveTake.passesBittensorRecommendationGates(netuid, validator.hotkey, gates))
        }
    }

    @Test
    fun `missing delegate entry reads the default take`() {
        val row = map(BittensorNetuid.Subnet(1), snapshot.copy(commissionsInU16 = emptyMap()))

        assertEquals(validatorCommissionRateFromU16(BigInteger.valueOf(11796)), row.commissionRate)
        assertTrue(row.canSelect)
    }

    @Test
    fun `missing chain status is selectable but never recommended`() {
        val missing = snapshot.copy(permits = emptyList(), lastUpdates = emptyList())
        val row = map(BittensorNetuid.Subnet(1), missing)

        assertFalse(row.inactive)
        assertTrue(row.canSelect)
        val disabledGates = gates.copy(requirePermit = false, requireActive = false)
        assertFalse(missing.passesBittensorRecommendationGates(BittensorNetuid.Subnet(1), validator.hotkey, disabledGates))
    }

    @Test
    fun `recommendation gates follow the backend permit and activity flags`() {
        val subnet = BittensorNetuid.Subnet(1)
        val disabledGates = gates.copy(requirePermit = false, requireActive = false)
        val withoutPermit = snapshot.copy(permits = listOf(false))
        val stale = snapshot.copy(lastUpdates = listOf(BigInteger.ZERO))

        assertFalse(withoutPermit.passesBittensorRecommendationGates(subnet, validator.hotkey, gates))
        assertFalse(stale.passesBittensorRecommendationGates(subnet, validator.hotkey, gates))
        assertTrue(withoutPermit.passesBittensorRecommendationGates(subnet, validator.hotkey, disabledGates))
        assertTrue(stale.passesBittensorRecommendationGates(subnet, validator.hotkey, disabledGates))
        assertFalse(snapshot.copy(uids = emptyMap()).passesBittensorRecommendationGates(subnet, validator.hotkey, disabledGates))
    }

    @Test
    fun `root ignores subnet-only status but recommends only registered validators`() {
        val root = snapshot.copy(permits = emptyList(), lastUpdates = emptyList())

        assertTrue(map(BittensorNetuid.Root, root).canSelect)
        assertTrue(map(BittensorNetuid.Root, root.copy(uids = emptyMap())).canSelect)
        assertTrue(root.passesBittensorRecommendationGates(BittensorNetuid.Root, validator.hotkey, gates))
        assertFalse(root.copy(uids = emptyMap()).passesBittensorRecommendationGates(BittensorNetuid.Root, validator.hotkey, gates))
    }

    @Test
    fun `raw delegate commission maps to a decimal rate`() {
        val expectedRates = mapOf(0 to "0", 13107 to "0.2", 65535 to "1")

        expectedRates.forEach { (rawCommission, expectedRate) ->
            val commissionSnapshot = snapshot.copy(commissionsInU16 = mapOf(validator.hotkey to rawCommission.toBigInteger()))

            assertEquals(0, BigDecimal(expectedRate).compareTo(map(BittensorNetuid.Root, commissionSnapshot).commissionRate))
        }
    }

    private fun map(netuid: BittensorNetuid, snapshot: BittensorValidatorSnapshot) =
        mapBittensorValidators(chain, netuid, listOf(validator), snapshot).single()
}
