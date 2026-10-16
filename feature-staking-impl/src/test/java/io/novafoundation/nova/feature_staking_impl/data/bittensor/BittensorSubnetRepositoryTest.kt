package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class BittensorSubnetRepositoryTest {

    @Test
    fun `catalogue removes null and root entries and sorts by netuid`() {
        val subnets = bindBittensorSubnets(listOf(null, subnet(10), subnet(0), subnet(1)))
        assertEquals(listOf(1, 10), subnets.map { it.id.value })
        assertEquals("Subnet name", subnets.first().name)
        assertEquals("ش", subnets.first().symbol)
    }

    @Test
    fun `compact utf8 and byte array text are both decoded`() {
        val bytes = "ش".encodeToByteArray()
        assertEquals("ش", bindBittensorText(bytes))
        assertEquals("ش", bindBittensorText(bytes.map { BigInteger.valueOf((it.toInt() and 255).toLong()) }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `duplicate subnet ids are rejected`() {
        bindBittensorSubnets(listOf(subnet(1), subnet(1)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `compact byte outside u8 is rejected`() {
        bindBittensorText(listOf(BigInteger.valueOf(256)))
    }

    @Test
    fun `only https logos are accepted`() {
        val safe = subnet(1, "https://example.com/subnet.png")
        val unsafe = subnet(2, "file:///private/logo.png")
        val subnets = bindBittensorSubnets(listOf(safe, unsafe))
        assertEquals("https://example.com/subnet.png", subnets[0].logoUrl)
        assertEquals(null, subnets[1].logoUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `wrong field names do not silently turn into unnamed subnets`() {
        bindBittensorSubnets(listOf(Struct.Instance(mapOf("netuid" to BigInteger.ONE, "subnet_name" to byteArrayOf()))))
    }

    @Test
    fun `explicitly empty runtime names remain empty for the UI fallback`() {
        val value = Struct.Instance(subnet(1).mapping + mapOf("subnetName" to byteArrayOf(), "tokenSymbol" to byteArrayOf()))
        val subnet = bindBittensorSubnets(listOf(value)).single()
        assertEquals("", subnet.name)
        assertEquals("", subnet.symbol)
    }

    @Test
    fun `on chain ratio is rao per alpha with nine decimals`() {
        assertEquals(BigInteger.valueOf(41_200_000), bindBittensorSubnetPrice(BigInteger.valueOf(4_120_000_000), BigInteger.valueOf(100_000_000_000)))
    }

    @Test
    fun `empty liquidity has no price instead of a zero price`() {
        assertEquals(null, bindBittensorSubnetPrice(BigInteger.ZERO, BigInteger.TEN))
        assertEquals(null, bindBittensorSubnetPrice(BigInteger.TEN, BigInteger.ZERO))
        assertEquals(null, bindBittensorSubnetPrice(null, null))
    }

    private fun subnet(id: Long, logo: String? = null) = Struct.Instance(
        mapOf(
            "netuid" to BigInteger.valueOf(id),
            "subnetName" to "Subnet name".encodeToByteArray(),
            "tokenSymbol" to "ش".encodeToByteArray().map { BigInteger.valueOf((it.toInt() and 255).toLong()) },
            "subnetIdentity" to logo?.let { Struct.Instance(mapOf("logoUrl" to it.encodeToByteArray())) }
        )
    )
}
