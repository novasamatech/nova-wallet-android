package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorPortfolioPositionsTest {

    private val firstValidator = AccountIdKey(ByteArray(32))
    private val secondValidator = AccountIdKey(ByteArray(32) { 1 })
    private val subnet = BittensorNetuid.Subnet(1)

    @Test
    fun `root stake includes all validators`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(firstValidator, n(10)), BittensorRootPosition(secondValidator, n(20))),
            emptyList(),
            emptyMap()
        )
        assertEquals(n(30), positions.rootStake)
        assertEquals(n(30), positions.totalStake)
    }

    @Test
    fun `subnet valuation aggregates alpha before rounding`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(firstValidator, n(10))),
            listOf(BittensorSubnetPosition(subnet, firstValidator, n(1)), BittensorSubnetPosition(subnet, secondValidator, n(1))),
            mapOf(subnet to n(500_000_000))
        )
        assertEquals(n(1), positions.subnetStake(subnet))
        assertEquals(n(11), positions.totalStake)
    }

    @Test
    fun `missing price never produces a partial total`() {
        val positions = BittensorPortfolioPositions(
            listOf(BittensorRootPosition(firstValidator, n(10))),
            listOf(BittensorSubnetPosition(subnet, firstValidator, n(1))),
            emptyMap()
        )
        assertEquals(n(10), positions.rootStake)
        assertNull(positions.subnetStake(subnet))
        assertNull(positions.totalStake)
    }

    @Test
    fun `successfully loaded empty portfolio is zero`() {
        val positions = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())
        assertEquals(BigInteger.ZERO, positions.totalStake)
    }

    private fun n(value: Long) = BigInteger.valueOf(value)
}
