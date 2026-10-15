package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import io.novafoundation.nova.feature_deep_linking.presentation.handling.common.DeepLinkHandlingException.StakingHandlingException
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.MockitoJUnitRunner

private const val CHAIN_ID = "2f0555cc76fc2840a25a6ea3b9637146806f1f44b090c175ffde2a7e5ab36c03"
private const val STAKING_ASSET_ID = 0
private const val PLAIN_ASSET_ID = 1

@RunWith(MockitoJUnitRunner::class)
class StakingDeepLinkTargetResolverTest {

    @Mock
    lateinit var chainRegistry: ChainRegistry

    @Mock
    lateinit var chain: Chain

    @Mock
    lateinit var stakingAsset: Chain.Asset

    @Mock
    lateinit var plainAsset: Chain.Asset

    private lateinit var resolver: StakingDeepLinkTargetResolver

    @Before
    fun setup() {
        lenient().`when`(stakingAsset.id).thenReturn(STAKING_ASSET_ID)
        lenient().`when`(stakingAsset.staking).thenReturn(listOf(Chain.Asset.StakingType.BITTENSOR_ROOT))

        lenient().`when`(plainAsset.id).thenReturn(PLAIN_ASSET_ID)
        lenient().`when`(plainAsset.staking).thenReturn(emptyList())

        lenient().`when`(chain.id).thenReturn(CHAIN_ID)
        lenient().`when`(chain.connectionState).thenReturn(Chain.ConnectionState.FULL_SYNC)
        lenient().`when`(chain.assetsById).thenReturn(mapOf(STAKING_ASSET_ID to stakingAsset, PLAIN_ASSET_ID to plainAsset))

        lenient().`when`(chainRegistry.chainsById).thenReturn(MutableStateFlow(mapOf(CHAIN_ID to chain)))

        resolver = StakingDeepLinkTargetResolver(chainRegistry)
    }

    @Test
    fun `link without chain opens the plain dashboard`() = runBlocking {
        assertNull(resolver.resolve(chainIdParam = null, assetIdParam = null))
        assertNull(resolver.resolve(chainIdParam = "", assetIdParam = "0"))
    }

    @Test
    fun `chain and asset resolve to the staking asset`() = runBlocking {
        val target = resolver.resolve(chainIdParam = CHAIN_ID, assetIdParam = "0")

        assertEquals(FullChainAssetId(CHAIN_ID, STAKING_ASSET_ID), target)
    }

    @Test
    fun `asset defaults to the native one`() = runBlocking {
        val target = resolver.resolve(chainIdParam = CHAIN_ID, assetIdParam = null)

        assertEquals(FullChainAssetId(CHAIN_ID, STAKING_ASSET_ID), target)
    }

    @Test
    fun `unknown chain fails`() = runBlocking {
        assertFailsWith(StakingHandlingException.ChainIsNotFound) {
            resolver.resolve(chainIdParam = "deadbeef", assetIdParam = null)
        }
    }

    @Test
    fun `disabled chain fails`() = runBlocking {
        lenient().`when`(chain.connectionState).thenReturn(Chain.ConnectionState.DISABLED)

        assertFailsWith(StakingHandlingException.ChainIsNotFound) {
            resolver.resolve(chainIdParam = CHAIN_ID, assetIdParam = null)
        }
    }

    @Test
    fun `unknown or malformed asset fails`() = runBlocking {
        assertFailsWith(StakingHandlingException.AssetIsNotFound) {
            resolver.resolve(chainIdParam = CHAIN_ID, assetIdParam = "7")
        }

        assertFailsWith(StakingHandlingException.AssetIsNotFound) {
            resolver.resolve(chainIdParam = CHAIN_ID, assetIdParam = "tao")
        }
    }

    @Test
    fun `asset without staking fails`() = runBlocking {
        assertFailsWith(StakingHandlingException.StakingIsNotSupported) {
            resolver.resolve(chainIdParam = CHAIN_ID, assetIdParam = PLAIN_ASSET_ID.toString())
        }
    }

    private suspend fun assertFailsWith(expected: StakingHandlingException, block: suspend () -> Unit) {
        try {
            block()
            fail("Expected $expected")
        } catch (e: StakingHandlingException) {
            assertEquals(expected, e)
        }
    }
}
