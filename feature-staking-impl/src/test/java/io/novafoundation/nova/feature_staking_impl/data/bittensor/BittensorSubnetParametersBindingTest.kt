package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorSubnetParametersBindingTest {

    @Test
    fun `emission binding preserves root null rows and duplicate identities`() {
        val rows = bindBittensorEmissionRows(listOf(row(0), null, row(1), row(1)))
        assertEquals(4, rows.size)
        assertEquals(BittensorNetuid.Root, rows[0]?.netuid)
        assertEquals(123.toBigInteger(), rows[0]?.taoInEmission)
        assertNull(rows[1])
        assertEquals(rows[2], rows[3])
    }

    @Test
    fun `missing optional fields stay unknown`() {
        val decoded = Struct.Instance(mapOf("netuid" to BigInteger.ONE))
        val row = bindBittensorEmissionRows(listOf(decoded)).single()!!
        assertNull(row.registrationBlock)
        assertNull(row.taoInEmission)
    }

    @Test
    fun `metadata camel case fields are decoded exactly`() {
        val row = bindBittensorEmissionRows(listOf(row(1))).single()!!
        assertEquals(42.toBigInteger(), row.registrationBlock)
        assertEquals(123.toBigInteger(), row.taoInEmission)
    }

    private fun row(netuid: Int) = Struct.Instance(
        mapOf(
            "netuid" to netuid.toBigInteger(),
            "taoInEmission" to 123.toBigInteger(),
            "networkRegisteredAt" to 42.toBigInteger()
        )
    )
}
