package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioMappingTest {

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private val subnet = BittensorNetuid.Subnet(1)

    @Test
    fun `held subnet ids deduplicate validators and omit zero positions`() {
        val anotherHotkey = AccountIdKey(ByteArray(32) { 2 })
        val positions = BittensorStakePositions(
            emptyList(),
            listOf(
                BittensorSubnetPosition(subnet, hotkey, BigInteger.ONE),
                BittensorSubnetPosition(subnet, anotherHotkey, BigInteger.TEN),
                BittensorSubnetPosition(BittensorNetuid.Subnet(2), hotkey, BigInteger.ZERO)
            )
        )

        assertEquals(setOf(subnet), positions.heldSubnets())
    }

    @Test
    fun `partial prices preserve all holdings without reporting a partial total`() {
        val otherSubnet = BittensorNetuid.Subnet(2)
        val positions = BittensorStakePositions(
            emptyList(),
            listOf(
                BittensorSubnetPosition(subnet, hotkey, BigInteger.ONE),
                BittensorSubnetPosition(otherSubnet, hotkey, BigInteger.ONE)
            )
        )
        val prices = mapOf(subnet to BigInteger.TEN)

        val portfolio = mapBittensorPortfolio(positions, prices)

        assertEquals(positions.subnets, portfolio.subnets)
        assertEquals(prices, portfolio.subnetPrices)
        assertNull(portfolio.totalStake)
    }

    @Test
    fun `unavailable prices preserve holdings with unknown valuation`() {
        val positions = BittensorStakePositions(emptyList(), listOf(BittensorSubnetPosition(subnet, hotkey, BigInteger.ONE)))

        val portfolio = mapBittensorPortfolio(positions, emptyMap())

        assertEquals(positions.subnets, portfolio.subnets)
        assertNull(portfolio.totalStake)
    }

    @Test
    fun `root holdings and an empty portfolio need no subnet prices`() {
        val root = BittensorRootPosition(hotkey, BigInteger.TEN)
        val positions = BittensorStakePositions(listOf(root), emptyList())
        val empty = BittensorStakePositions(emptyList(), emptyList())

        assertTrue(positions.heldSubnets().isEmpty())
        assertEquals(BigInteger.TEN, mapBittensorPortfolio(positions, emptyMap()).totalStake)
        assertEquals(BigInteger.ZERO, mapBittensorPortfolio(empty, emptyMap()).totalStake)
        assertTrue((null as BittensorStakePositions?).heldSubnets().isEmpty())
    }
}
