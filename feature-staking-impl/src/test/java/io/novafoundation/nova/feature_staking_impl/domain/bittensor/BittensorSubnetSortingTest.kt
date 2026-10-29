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
    fun `unknown registration sorts after known ages`() {
        val newer = subnet(1, 10, 20)
        val unknown = subnet(2, 10, 1).copy(registrationBlock = null)
        val older = subnet(3, 10, 10)

        assertEquals(listOf(3, 1, 2), sort(listOf(newer, unknown, older), BittensorSubnetSorting.AGE))
    }

    @Test
    fun `age sorting keeps favorites first before sorting unknown registration last`() {
        val favorite = subnet(1, 10, 20)
        val unknown = subnet(2, 10, 1).copy(registrationBlock = null)
        val older = subnet(3, 10, 10)
        val favorites = BittensorSubnetFavorites(metaId = 1, setOf(BittensorSubnetReference(favorite.id, requireNotNull(favorite.registrationBlock))))

        val sorted = sortBittensorSubnets(
            listOf(unknown, older, favorite),
            BittensorSubnetSorting.AGE,
            favorites,
            hideThinPools = false,
            yields = emptyMap(),
            weekly = emptyMap(),
            previousOrder = emptyList(),
            Locale.US,
            subnetNumber
        )

        assertEquals(listOf(1, 3, 2), sorted.map { it.id.value })
    }

    @Test
    fun `thin threshold is inclusive and only matching registration favorite bypasses it`() {
        val thin = subnet(1, 1, 1)
        val boundary = subnet(2, 2000, 1)
        val replaced = subnet(3, 1, 2)
        val favorites = BittensorSubnetFavorites(
            metaId = 1,
            setOf(BittensorSubnetReference(thin.id, 1.toBigInteger()), BittensorSubnetReference(replaced.id, 1.toBigInteger()))
        )
        val rows = sortBittensorSubnets(
            listOf(thin, boundary, replaced),
            BittensorSubnetSorting.POOL_DEPTH,
            favorites,
            hideThinPools = true,
            yields = emptyMap(),
            weekly = emptyMap(),
            previousOrder = emptyList(),
            Locale.US,
            subnetNumber
        )
        assertEquals(listOf(1, 2), rows.map { it.id.value })
    }

    @Test
    fun `equal metrics preserve previous identity order`() {
        val rows = listOf(subnet(1, 10, 1), subnet(2, 10, 1))
        val previous = rows.reversed().map { BittensorSubnetReference(it.id, requireNotNull(it.registrationBlock)) }
        val favorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())
        val sorted = sortBittensorSubnets(
            rows,
            BittensorSubnetSorting.POOL_DEPTH,
            favorites,
            hideThinPools = false,
            yields = emptyMap(),
            weekly = emptyMap(),
            previous,
            Locale.US,
            subnetNumber
        )
        assertEquals(listOf(2, 1), sorted.map { it.id.value })
    }

    @Test
    fun `name order compares shown titles case-insensitively with numeric digit runs`() {
        val rows = listOf(named(1, "beta"), named(2, ""), named(3, "Alpha"), named(10, "  "), named(4, "Subnet 3"))
        assertEquals(listOf(3, 1, 2, 4, 10), sort(rows, BittensorSubnetSorting.NAME))
    }

    @Test
    fun `weekly changes sort descending with missing values after negative values`() {
        val rows = listOf(subnet(1, 10, 1), subnet(2, 10, 1), subnet(3, 10, 1), subnet(4, 10, 1))
        val weekly = mapOf(rows[0].id to BigDecimal.ZERO, rows[2].id to BigDecimal.ONE, rows[3].id to BigDecimal("-1"))
        val favorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())

        val sorted = sortBittensorSubnets(
            rows,
            BittensorSubnetSorting.SEVEN_DAY_CHANGE,
            favorites,
            hideThinPools = false,
            yields = emptyMap(),
            weekly,
            previousOrder = emptyList(),
            Locale.US,
            subnetNumber
        )

        assertEquals(listOf(3, 1, 4, 2), sorted.map { it.id.value })
    }

    @Test
    fun `missing pool reserve sorts last and is hidden unless favorited`() {
        val unknown = subnet(1, 10, 1).copy(taoReserveInPlanks = null)
        val known = subnet(2, 2000, 1)
        val rows = listOf(unknown, known)
        assertEquals(listOf(2, 1), sort(rows, BittensorSubnetSorting.POOL_DEPTH))
        val noFavorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())
        val favoriteReference = BittensorSubnetReference(unknown.id, requireNotNull(unknown.registrationBlock))
        val favorites = BittensorSubnetFavorites(metaId = 1, setOf(favoriteReference))

        assertEquals(false, passesBittensorThinPoolFilter(unknown, noFavorites))
        assertEquals(true, passesBittensorThinPoolFilter(unknown, favorites))
        assertEquals(true, passesBittensorThinPoolFilter(known, noFavorites))
    }

    @Test
    fun `selected metric takes priority over the previous order`() {
        val rows = listOf(subnet(1, 10, 1), subnet(2, 20, 1))
        val previous = rows.map { BittensorSubnetReference(it.id, requireNotNull(it.registrationBlock)) }
        val favorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())

        val sorted = sortBittensorSubnets(
            rows,
            BittensorSubnetSorting.POOL_DEPTH,
            favorites,
            hideThinPools = false,
            yields = emptyMap(),
            weekly = emptyMap(),
            previous,
            Locale.US,
            subnetNumber
        )

        assertEquals(listOf(2, 1), sorted.map { it.id.value })
    }

    @Test
    fun `previous order does not attach to a replaced or unknown registration`() {
        val replaced = subnet(1, 10, 2)
        val retained = subnet(2, 10, 1)
        val unknown = subnet(3, 10, 1).copy(registrationBlock = null)
        val previous = listOf(
            BittensorSubnetReference(replaced.id, 1.toBigInteger()),
            BittensorSubnetReference(unknown.id, 1.toBigInteger()),
            BittensorSubnetReference(retained.id, requireNotNull(retained.registrationBlock))
        )
        val favorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())

        val sorted = sortBittensorSubnets(
            listOf(unknown, replaced, retained),
            BittensorSubnetSorting.POOL_DEPTH,
            favorites,
            hideThinPools = false,
            yields = emptyMap(),
            weekly = emptyMap(),
            previous,
            Locale.US,
            subnetNumber
        )

        assertEquals(listOf(2, 1, 3), sorted.map { it.id.value })
    }

    @Test
    fun `unranked ties are resolved by subnet id and then registration`() {
        val older = subnet(1, 10, 1)
        val newer = subnet(1, 10, 2)
        val unknown = older.copy(registrationBlock = null)
        val other = subnet(2, 10, 1)
        val favorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())

        val sorted = sortBittensorSubnets(
            listOf(other, newer, unknown, older),
            BittensorSubnetSorting.POOL_DEPTH,
            favorites,
            hideThinPools = false,
            yields = emptyMap(),
            weekly = emptyMap(),
            previousOrder = emptyList(),
            Locale.US,
            subnetNumber
        )

        assertEquals(listOf(unknown, older, newer, other), sorted)
    }

    private fun sort(rows: List<BittensorSubnet>, order: BittensorSubnetSorting, rates: Map<BittensorNetuid.Subnet, BigDecimal> = emptyMap()): List<Int> {
        val favorites = BittensorSubnetFavorites(metaId = 1, subnets = emptySet())
        return sortBittensorSubnets(
            rows,
            order,
            favorites,
            hideThinPools = false,
            rates,
            weekly = emptyMap(),
            previousOrder = emptyList(),
            Locale.US,
            subnetNumber
        ).map { it.id.value }
    }

    private fun named(id: Int, name: String) = subnet(id, 10, id.toLong()).copy(name = name)

    private fun subnet(id: Int, pool: Long, registration: Long) = BittensorSubnet(
        BittensorNetuid.Subnet(id),
        "Subnet $id",
        "S$id",
        logoUrl = null,
        taoReserveInPlanks = pool.toBigInteger() * 1_000_000_000.toBigInteger(),
        registrationBlock = registration.toBigInteger()
    )
}
