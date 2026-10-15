package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidator
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
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
        takes = mapOf(validator.hotkey to BigInteger.ZERO),
        permits = listOf(true),
        lastUpdates = listOf(BigInteger.TEN),
        activeThreshold = BigInteger.TEN
    )

    @Test
    fun `subnet permit and activity gates are evaluated from the same snapshot`() {
        assertTrue(mapBittensorValidators(chain, 1, listOf(validator), gates, snapshot).single().selectable)
        assertTrue(mapBittensorValidators(chain, 1, listOf(validator), gates, snapshot.copy(permits = listOf(false))).isEmpty())
        val inactive = snapshot.copy(lastUpdates = listOf(BigInteger.ONE))
        assertFalse(mapBittensorValidators(chain, 1, listOf(validator), gates, inactive).single().selectable)
    }

    @Test
    fun `root ignores subnet-only gates but requires registration and an allowed take`() {
        val root = snapshot.copy(permits = emptyList(), lastUpdates = emptyList())
        assertTrue(mapBittensorValidators(chain, 0, listOf(validator), gates, root).single().selectable)
        assertTrue(mapBittensorValidators(chain, 0, listOf(validator), gates, root.copy(uids = emptyMap())).isEmpty())
        val excessiveTake = root.copy(takes = mapOf(validator.hotkey to BigInteger.valueOf(65535)))
        assertTrue(mapBittensorValidators(chain, 0, listOf(validator), gates, excessiveTake).isEmpty())
    }
}
