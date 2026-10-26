package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.math.BigDecimal
import kotlin.random.Random

class BittensorQuickPickSelectorTest {

    private val random = Random(1)

    private val selector = BittensorQuickPickSelector(random)

    @Test
    fun `pool candidates are the ten largest pools`() {
        val subnets = (1..12).map { subnet(it, pool = 2000L + it, registered = it.toLong()) }

        assertEquals((12 downTo 3).toList(), ids(selector.candidates(subnets, BittensorQuickPickOrder.POOL_SIZE)))
    }

    @Test
    fun `age candidates are the ten oldest subnets`() {
        val subnets = (1..12).reversed().map { subnet(it, pool = 2000L, registered = it.toLong()) }

        assertEquals((1..10).toList(), ids(selector.candidates(subnets, BittensorQuickPickOrder.AGE)))
    }

    @Test
    fun `thin pools are excluded and threshold is inclusive`() {
        val subnets = listOf(subnet(1, 1999L, 1L), subnet(2, 2000L, 2L))

        assertEquals(listOf(2), ids(selector.candidates(subnets, BittensorQuickPickOrder.AGE)))
    }

    @Test
    fun `ties use subnet id regardless of source order`() {
        val subnets = listOf(subnet(3), subnet(1), subnet(2))
        val annualYields = subnets.associate { it.id to BigDecimal.TEN }

        BittensorQuickPickOrder.entries.forEach { order ->
            assertEquals(listOf(1, 2, 3), ids(selector.candidates(subnets, order, annualYields)))
        }
    }

    @Test
    fun `selection retains registration and only chooses from candidate set`() {
        val subnets = (1..12).map { subnet(it, 2000L + it, it.toLong()) }
        val selected = selector.select(subnets, BittensorQuickPickOrder.POOL_SIZE)

        assertTrue(selected.subnetId.value in 3..12)
        assertEquals(selected.subnetId.value.toBigInteger(), selected.registrationBlock)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `missing registration must not silently reorder candidates`() {
        selector.select(listOf(subnet(1).copy(registrationBlock = null)), BittensorQuickPickOrder.AGE)
    }

    @Test
    fun `apy selects top ten known rates and excludes thin pools`() {
        val subnets = (1..14).map { subnet(it, pool = if (it == 14) 1999L else 2000L) }
        val annualYields = subnets
            .filter { it.id.value != 13 }
            .associate { it.id to it.id.value.toBigDecimal() }

        assertEquals((12 downTo 3).toList(), ids(selector.candidates(subnets, BittensorQuickPickOrder.APY, annualYields)))
    }

    @Test
    fun `zero apy is known but missing apy is not`() {
        val subnets = listOf(subnet(1), subnet(2))
        val annualYields = mapOf(subnets.first().id to BigDecimal.ZERO)

        assertEquals(listOf(1), ids(selector.candidates(subnets, BittensorQuickPickOrder.APY, annualYields)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no eligible pools is not a random fallback`() {
        selector.select(listOf(subnet(1, pool = 1L)), BittensorQuickPickOrder.POOL_SIZE)
    }

    private fun ids(references: List<BittensorSubnetReference>) = references.map { it.subnetId.value }

    private fun subnet(id: Int, pool: Long = 2000L, registered: Long = 1L) = BittensorSubnet(
        id = BittensorNetuid.Subnet(id),
        name = "Subnet $id",
        symbol = "α",
        logoUrl = null,
        taoReserveInPlanks = pool.toBigInteger() * BigInteger.TEN.pow(9),
        registrationBlock = registered.toBigInteger()
    )
}
