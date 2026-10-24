package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetFavorites
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.util.Locale

class BittensorSubnetSortingTest {

    private val subnetNumber: (Int) -> String = { "Subnet $it" }

    @Test
    fun `pool age and APY orders retain unknown yields last`() {
        val rows = listOf(subnet(1, 30, 3), subnet(2, 10, 1), subnet(3, 20, 2))
        assertEquals(listOf(1, 3, 2), sort(rows, BittensorSubnetSorting.POOL_DEPTH))
        assertEquals(listOf(2, 3, 1), sort(rows, BittensorSubnetSorting.AGE))
        val rates = mapOf(rows[0].id to BigDecimal.ONE, rows[2].id to BigDecimal.TEN)
        assertEquals(listOf(3, 1, 2), sort(rows, BittensorSubnetSorting.APY, rates))
    }

    @Test
    fun `thin threshold is inclusive and only matching registration favorite bypasses it`() {
        val thin = subnet(1, 1, 1)
        val boundary = subnet(2, 2000, 1)
        val replaced = subnet(3, 1, 2)
        val favorites = BittensorSubnetFavorites(
            1,
            setOf(BittensorSubnetReference(thin.id, 1.toBigInteger()), BittensorSubnetReference(replaced.id, 1.toBigInteger()))
        )
        val rows = sortBittensorSubnets(
            listOf(thin, boundary, replaced),
            BittensorSubnetSorting.POOL_DEPTH,
            favorites,
            true,
            emptyMap(),
            emptyMap(),
            emptyList(),
            Locale.US,
            subnetNumber
        )
        assertEquals(listOf(1, 2), rows.map { it.id.value })
    }

    @Test
    fun `equal metrics preserve previous identity order`() {
        val rows = listOf(subnet(1, 10, 1), subnet(2, 10, 1))
        val previous = rows.reversed().map { BittensorSubnetReference(it.id, requireNotNull(it.registrationBlock)) }
        val favorites = BittensorSubnetFavorites(1, emptySet())
        val sorted = sortBittensorSubnets(rows, BittensorSubnetSorting.POOL_DEPTH, favorites, false, emptyMap(), emptyMap(), previous, Locale.US, subnetNumber)
        assertEquals(listOf(2, 1), sorted.map { it.id.value })
    }

    @Test
    fun `name order compares shown titles case-insensitively with numeric digit runs`() {
        val rows = listOf(named(1, "beta"), named(2, ""), named(3, "Alpha"), named(10, "  "), named(4, "Subnet 3"))
        assertEquals(listOf(3, 1, 2, 4, 10), sort(rows, BittensorSubnetSorting.NAME))
    }

    private fun sort(rows: List<BittensorSubnet>, order: BittensorSubnetSorting, rates: Map<BittensorNetuid.Subnet, BigDecimal> = emptyMap()): List<Int> {
        val favorites = BittensorSubnetFavorites(1, emptySet())
        return sortBittensorSubnets(rows, order, favorites, false, rates, emptyMap(), emptyList(), Locale.US, subnetNumber).map { it.id.value }
    }

    private fun named(id: Int, name: String) = subnet(id, 10, id.toLong()).copy(name = name)

    private fun subnet(id: Int, pool: Long, registration: Long) = BittensorSubnet(
        BittensorNetuid.Subnet(id),
        "Subnet $id",
        "S$id",
        null,
        taoReserveInPlanks = pool.toBigInteger() * 1_000_000_000.toBigInteger(),
        registrationBlock = registration.toBigInteger()
    )
}
