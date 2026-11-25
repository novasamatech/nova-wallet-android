package io.novafoundation.nova.feature_dapp_impl.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DAppStakingDetectionTest {

    @Test
    fun `should detect staking keywords as token prefixes`() {
        assertTrue(DAppStakingDetection.isStakingQuery("staking"))
        assertTrue(DAppStakingDetection.isStakingQuery("Staker"))
        assertTrue(DAppStakingDetection.isStakingQuery("best stake rewards"))
        assertTrue(DAppStakingDetection.isStakingQuery("стейкинг"))
    }

    @Test
    fun `should not match keywords in the middle of a word`() {
        assertFalse(DAppStakingDetection.isStakingQuery("mistake"))
        assertFalse(DAppStakingDetection.isStakingQuery("by mistake"))
    }

    @Test
    fun `should detect CJK staking terms as substrings`() {
        assertTrue(DAppStakingDetection.isStakingQuery("质押"))
        assertTrue(DAppStakingDetection.isStakingQuery("波卡质押平台"))
        assertTrue(DAppStakingDetection.isStakingQuery("ステーキング"))
        assertTrue(DAppStakingDetection.isStakingQuery("스테이킹"))
    }

    @Test
    fun `should treat blank query as non staking`() {
        assertFalse(DAppStakingDetection.isStakingQuery(null))
        assertFalse(DAppStakingDetection.isStakingQuery(""))
    }

    @Test
    fun `should detect staking host by labels`() {
        assertTrue(DAppStakingDetection.isStakingHost("staking.polkadot.network"))
        assertTrue(DAppStakingDetection.isStakingHost("stakewise.io"))
        assertTrue(DAppStakingDetection.isStakingHost("STAKING.EXAMPLE.COM"))
    }

    @Test
    fun `should not detect unrelated host as staking`() {
        assertFalse(DAppStakingDetection.isStakingHost("polkadot.network"))
        assertFalse(DAppStakingDetection.isStakingHost("mistake.io"))
        assertFalse(DAppStakingDetection.isStakingHost(null))
        assertFalse(DAppStakingDetection.isStakingHost(""))
    }

    @Test
    fun `should detect TAO related hosts`() {
        val bittensorHosts = listOf(
            "taostats.io",
            "dash.taostats.io",
            "tao.app",
            "www.tao.app",
            "tensorwallet.ca",
            "mentatminds.com",
            "mentatlend.com",
            "taoswap.org",
            "taomarketcap.com",
            "backprop.finance",
            "tao.com",
            "tao.bot",
            "potatao.io",
            "taoyield.com",
            "bittensor.com",
            "bridge.bittensor.com",
            "delegate.tensorplex.ai",
            "TAOSTATS.IO"
        )

        bittensorHosts.forEach { host ->
            assertTrue(host, DAppStakingDetection.isBittensorHost(host))
        }
    }

    @Test
    fun `should not detect unrelated hosts as TAO related`() {
        val otherHosts = listOf(
            "taobao.com",
            "www.taobao.com",
            "tensor.trade",
            "tensorflow.org",
            "web.subwallet.app",
            "staking.polkadot.cloud",
            "app.hydration.net",
            null,
            ""
        )

        otherHosts.forEach { host ->
            assertFalse(host, DAppStakingDetection.isBittensorHost(host))
        }
    }

    @Test
    fun `should detect TAO related queries by words`() {
        val bittensorQueries = listOf("tao", "TAO staking", "bittensor", "taostats", "dtao", "taostats.io")

        bittensorQueries.forEach { query ->
            assertTrue(query, DAppStakingDetection.isBittensorQuery(query))
        }
    }

    @Test
    fun `should not detect unrelated queries as TAO related`() {
        val otherQueries = listOf("taobao", "tensor", "polkadot staking", "mistake", "", null)

        otherQueries.forEach { query ->
            assertFalse(query, DAppStakingDetection.isBittensorQuery(query))
        }
    }

    @Test
    fun `should prefer TAO over staking for a query matching both`() {
        assertEquals(DAppStakingQueryKind.BITTENSOR, DAppStakingDetection.queryKind("TAO staking"))
        assertEquals(DAppStakingQueryKind.BITTENSOR, DAppStakingDetection.queryKind("tao"))
        assertEquals(DAppStakingQueryKind.STAKING, DAppStakingDetection.queryKind("polkadot staking"))
        assertEquals(DAppStakingQueryKind.NONE, DAppStakingDetection.queryKind("hydration"))
        assertEquals(DAppStakingQueryKind.NONE, DAppStakingDetection.queryKind(""))
        assertEquals(DAppStakingQueryKind.NONE, DAppStakingDetection.queryKind(null))
    }

    @Test
    fun `should normalize host by dropping www and case`() {
        assertEquals("example.com", DAppStakingDetection.normalizeHost("WWW.Example.com"))
        assertEquals("example.com", DAppStakingDetection.normalizeHost("example.com"))
        assertEquals("wwwexample.com", DAppStakingDetection.normalizeHost("wwwexample.com"))
    }
}
