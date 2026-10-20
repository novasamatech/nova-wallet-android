package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkHandlingException.StakingHandlingException
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class StakingDeepLinkTargetResolverTest {

    private val registry = mock(ChainRegistry::class.java)

    private val chain = mock(Chain::class.java)

    private val asset = mock(Chain.Asset::class.java)

    private val resolver = StakingDeepLinkTargetResolver(registry)

    @Before
    fun prepare() {
        `when`(registry.chainsById).thenReturn(MutableStateFlow(mapOf("chain" to chain)))
        `when`(chain.id).thenReturn("chain")
        `when`(chain.connectionState).thenReturn(Chain.ConnectionState.FULL_SYNC)
        `when`(chain.assetsById).thenReturn(mapOf(0 to asset, 12 to asset))
        `when`(asset.id).thenReturn(0)
        `when`(asset.staking).thenReturn(listOf(Chain.Asset.StakingType.BITTENSOR_ROOT))
    }

    @Test
    fun `missing chain keeps plain dashboard`() = runBlocking {
        assertNull(resolver.resolve(null, null))
    }

    @Test
    fun `native asset default and hex prefix are preserved`() = runBlocking {
        assertEquals(StakingDeepLinkTarget("chain", 0), resolver.resolve("0xchain", null))
    }

    @Test
    fun `explicit staking asset is preserved`() = runBlocking {
        `when`(asset.id).thenReturn(12)
        assertEquals(StakingDeepLinkTarget("chain", 12), resolver.resolve("chain", "12"))
    }

    @Test
    fun `unknown and empty chain report typed error`() = runBlocking {
        assertFailure(StakingHandlingException.ChainIsNotFound) { resolver.resolve("unknown", null) }
        assertFailure(StakingHandlingException.ChainIsNotFound) { resolver.resolve(" ", null) }
    }

    @Test
    fun `disabled chain is rejected`() = runBlocking {
        `when`(chain.connectionState).thenReturn(Chain.ConnectionState.DISABLED)
        assertFailure(StakingHandlingException.ChainIsNotFound) { resolver.resolve("chain", null) }
    }

    @Test
    fun `missing malformed negative and overflowing asset ids are rejected`() = runBlocking {
        for (id in listOf("7", "tao", "-1", "2147483648", "")) {
            assertFailure(StakingHandlingException.AssetIsNotFound) { resolver.resolve("chain", id) }
        }
    }

    @Test
    fun `asset without staking is rejected`() = runBlocking {
        `when`(asset.staking).thenReturn(emptyList())
        assertFailure(StakingHandlingException.StakingIsNotSupported) { resolver.resolve("chain", "0") }
    }

    private suspend fun assertFailure(expected: StakingHandlingException, action: suspend () -> Unit) {
        try {
            action()
            fail("Expected $expected")
        } catch (error: StakingHandlingException) {
            assertEquals(expected, error)
        }
    }
}
