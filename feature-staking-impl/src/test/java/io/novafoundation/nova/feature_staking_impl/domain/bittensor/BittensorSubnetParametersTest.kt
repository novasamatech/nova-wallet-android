package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorSubnetParametersTest {

    private val reference = BittensorSubnetReference(BittensorNetuid.Subnet(1), 10.toBigInteger())

    @Test
    fun `root emission is included in complete denominator`() {
        val rows = listOf(row(0, 60), row(1, 20), row(2, 20))
        assertShare("0.2", rows)
    }

    @Test
    fun `null row makes enabled emission unknown`() {
        assertNull(share(listOf(row(1, 20), null)))
    }

    @Test
    fun `duplicates anywhere in enabled population make share unknown`() {
        assertNull(share(listOf(row(1, 20), row(2, 30), row(2, 30))))
        assertNull(share(listOf(row(1, 20), row(0, 30), row(0, 30))))
    }

    @Test
    fun `disabled emission is zero even if unrelated population is incomplete`() {
        assertShare("0", listOf(row(1, 20), null), enabled = false)
    }

    @Test
    fun `disabled emission still requires unique selected registration`() {
        assertNull(share(listOf(row(1, 20, registeredAt = 11)), enabled = false))
        assertNull(share(listOf(row(1, 20), row(1, 20)), enabled = false))
        assertNull(share(listOf(row(2, 20)), enabled = false))
    }

    @Test
    fun `unknown enabled flag is not disabled`() {
        assertNull(share(listOf(row(1, 20)), enabled = null))
    }

    @Test
    fun `missing selected registration is unknown`() {
        assertNull(share(listOf(row(1, 20).copy(registrationBlock = null))))
    }

    @Test
    fun `missing population emission is not zero`() {
        assertNull(share(listOf(row(1, 20), row(2, 20).copy(taoInEmission = null))))
    }

    @Test
    fun `zero total is unknown but zero selected share with positive total is valid`() {
        assertNull(share(listOf(row(1, 0), row(2, 0))))
        assertShare("0", listOf(row(1, 0), row(2, 20)))
    }

    @Test
    fun `population total can exceed u64 without overflow`() {
        val maximum = BigInteger.ONE.shiftLeft(64) - BigInteger.ONE
        val rows = listOf(row(1, 0).copy(taoInEmission = maximum), row(0, 0).copy(taoInEmission = maximum))
        assertShare("0.5", rows)
    }

    @Test
    fun `pool split preserves zero sides and rejects unknown or empty total`() {
        assertEquals(0, bittensorPoolNativeShare(3.toBigInteger(), 1.toBigInteger())!!.compareTo(BigDecimal("0.75")))
        assertEquals(0, bittensorPoolNativeShare(BigInteger.ZERO, BigInteger.ONE)!!.compareTo(BigDecimal.ZERO))
        assertEquals(0, bittensorPoolNativeShare(BigInteger.ONE, BigInteger.ZERO)!!.compareTo(BigDecimal.ONE))
        assertNull(bittensorPoolNativeShare(null, BigInteger.ONE))
        assertNull(bittensorPoolNativeShare(BigInteger.ZERO, BigInteger.ZERO))
    }

    @Test
    fun `alpha pool value uses protocol precision and floors raw native units`() {
        assertEquals(6.toBigInteger(), bittensorAlphaPoolValueInRao(2_000_000_001.toBigInteger(), 3.toBigInteger()))
        assertNull(bittensorAlphaPoolValueInRao(BigInteger.ONE, null))
        assertEquals(BigInteger.ZERO, bittensorAlphaPoolValueInRao(BigInteger.ZERO, BigInteger.ONE))
    }

    private fun row(id: Int, emission: Long, registeredAt: Long = 10) = BittensorSubnetEmissionRow(
        BittensorNetuid.from(id),
        registeredAt.toBigInteger(),
        emission.toBigInteger()
    )

    private fun share(rows: List<BittensorSubnetEmissionRow?>, enabled: Boolean? = true): BigDecimal? {
        val snapshot = BittensorSubnetEmissionSnapshot(rows, enabled)
        return bittensorSubnetEmissionShare(reference, snapshot)
    }

    private fun assertShare(expected: String, rows: List<BittensorSubnetEmissionRow?>, enabled: Boolean? = true) {
        assertEquals(0, requireNotNull(share(rows, enabled)).compareTo(BigDecimal(expected)))
    }
}
