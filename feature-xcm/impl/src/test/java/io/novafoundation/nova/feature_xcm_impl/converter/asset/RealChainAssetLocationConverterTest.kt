package io.novafoundation.nova.feature_xcm_impl.converter.asset

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_xcm_api.config.model.AssetsXcmConfig
import io.novafoundation.nova.feature_xcm_api.config.model.ChainAssetReserveConfig
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.GeneralIndex
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.PalletInstance
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.test_shared.whenever
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.math.BigInteger

@RunWith(MockitoJUnitRunner::class)
class RealChainAssetLocationConverterTest {

    @Mock
    private lateinit var xcmConfig: AssetsXcmConfig

    @Mock
    private lateinit var chainLocationConverter: ChainLocationConverter

    @Mock
    private lateinit var polkadot: Chain

    @Mock
    private lateinit var pah: Chain


    @Mock
    private lateinit var dotAssetOnPolkadot: Chain.Asset

    @Mock
    private lateinit var dotAssetOnPah: Chain.Asset

    @Mock
    private lateinit var usdcAssetOnPah: Chain.Asset

    private lateinit var converter: RealChainAssetLocationConverter

    // Chain IDs from real config
    private val polkadotChainId = "91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3"
    private val pahChainId = "68d56f15f85d3136970ec16946040bc1752654e906147f7e43e9d539d7c3de2f"

    // Asset IDs
    private val dotAssetId = 0
    private val usdcAssetId = 1337

    // Locations
    private val dotLocation = AbsoluteMultiLocation(Interior.Here)

    private val polkadotLocation = AbsoluteMultiLocation(Interior.Here)
    private val pahLocation = AbsoluteMultiLocation(ParachainId(1000))

    private val usdcLocation = AbsoluteMultiLocation(
        ParachainId(1000),
        PalletInstance(BigInteger.valueOf(50)),
        GeneralIndex(BigInteger.valueOf(1337))
    )

    @Before
    fun setUp() = runBlocking {
        setupChains()
        setupAssets()
        setupChainLocationConverter()
        setupAssetSearchBySymbol()
        setupXcmConfig()

        converter = RealChainAssetLocationConverter(xcmConfig, chainLocationConverter)
    }

    @Test
    fun `chainAssetFromRelativeLocation should find DOT asset on Polkadot when location points to Polkadot`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 0, interior = Interior.Here)

        val result = converter.chainAssetFromRelativeLocation(relativeLocation, polkadot)

        assertEquals(dotAssetOnPolkadot, result)
    }

    @Test
    fun `chainAssetFromRelativeLocation should find DOT asset on PAH when location points to PAH`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 1, interior = Interior.Here)

        val result = converter.chainAssetFromRelativeLocation(relativeLocation, pah)

        assertEquals(dotAssetOnPah, result)
    }

    @Test
    fun `chainAssetFromRelativeLocation should find USDC on PAH`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(
            parents = 0,
            interior = Junctions(
                PalletInstance(BigInteger.valueOf(50)),
                GeneralIndex(BigInteger.valueOf(1337))
            )
        )
        val result = converter.chainAssetFromRelativeLocation(relativeLocation, pah)
        assertEquals(usdcAssetOnPah, result)
    }

    @Test
    fun `chainAssetFromRelativeLocation should return null for unknown location`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(
            parents = 0,
            interior = Junctions(ParachainId(9999))
        )

        val result = converter.chainAssetFromRelativeLocation(relativeLocation, polkadot)

        assertNull(result)
    }

    @Test
    fun `absoluteLocationFromChainAsset should return Polkadot location for DOT on Polkadot`() = runBlocking {
        val result = converter.absoluteLocationFromChainAsset(dotAssetOnPolkadot)

        assertEquals(dotLocation, result)
    }

    @Test
    fun `absoluteLocationFromChainAsset should return PAH location for DOT on PAH`() = runBlocking {
        val result = converter.absoluteLocationFromChainAsset(dotAssetOnPah)

        assertEquals(dotLocation, result)
    }

    @Test
    fun `absoluteLocationFromChainAsset should return complex location for USDC on PAH`() = runBlocking {
        val result = converter.absoluteLocationFromChainAsset(usdcAssetOnPah)

        assertEquals(usdcLocation, result)
    }

    @Test
    fun `relativeLocationFromChainAsset should return relative location for DOT on Polkadot from PAH POV`() = runBlocking {
        val result = converter.relativeLocationFromChainAsset(dotAssetOnPolkadot)

        val expectedRelativeLocation = RelativeMultiLocation(parents = 0, interior = Interior.Here)
        assertEquals(expectedRelativeLocation, result)
    }

    @Test
    fun `relativeLocationFromChainAsset should return relative location for DOT on PAH from Polkadot POV`() = runBlocking {
        val result = converter.relativeLocationFromChainAsset(dotAssetOnPah)

        val expectedRelativeLocation = RelativeMultiLocation(parents = 1, interior = Interior.Here)
        assertEquals(expectedRelativeLocation, result)
    }

    @Test
    fun `relativeLocationFromChainAsset should return relative location for USDC with complex location`() = runBlocking {
        val result = converter.relativeLocationFromChainAsset(usdcAssetOnPah)

        val expectedRelativeLocation = RelativeMultiLocation(
            parents = 0,
            interior = Junctions(
                PalletInstance(BigInteger.valueOf(50)),
                GeneralIndex(BigInteger.valueOf(1337))
            )
        )
        assertEquals(expectedRelativeLocation, result)
    }

    private fun setupChains() {
        setupChain(polkadot, polkadotChainId, mapOf(dotAssetId to dotAssetOnPolkadot))
        setupChain(pah, pahChainId, mapOf(
            dotAssetId to dotAssetOnPah,
            usdcAssetId to usdcAssetOnPah
        ))
    }

    private fun setupAssets() {
        setupAsset(dotAssetOnPolkadot, polkadotChainId, dotAssetId, "DOT")
        setupAsset(dotAssetOnPah, pahChainId, dotAssetId, "DOT")
        setupAsset(usdcAssetOnPah, pahChainId, usdcAssetId, "USDC")
    }

    private fun setupChain(chain: Chain, chainId: String, assets: Map<Int, Chain.Asset>) {
        whenever(chain.id).thenReturn(chainId)
        whenever(chain.assetsById).thenReturn(assets)
    }

    private fun setupAsset(asset: Chain.Asset, chainId: String, assetId: Int, symbol: String) {
        whenever(asset.chainId).thenReturn(chainId)
        whenever(asset.id).thenReturn(assetId)
        whenever(asset.symbol).thenReturn(TokenSymbol(symbol))
    }

    private suspend fun setupChainLocationConverter() {
        whenever(chainLocationConverter.absoluteLocationFromChain(polkadotChainId)).thenReturn(polkadotLocation)
        whenever(chainLocationConverter.absoluteLocationFromChain(pahChainId)).thenReturn(pahLocation)
    }

    private fun setupAssetSearchBySymbol() {
        whenever(polkadot.assets).thenReturn(listOf(dotAssetOnPolkadot))
        whenever(pah.assets).thenReturn(listOf(dotAssetOnPah, usdcAssetOnPah))
    }

    private fun setupXcmConfig() {
        val dotReserve = ChainAssetReserveConfig(
            reserveId = "DOT",
            reserveAssetId = FullChainAssetId(polkadotChainId, dotAssetId),
            tokenLocation = dotLocation
        )

        val dotPahReserve = ChainAssetReserveConfig(
            reserveId = "DOT-PAH",
            reserveAssetId = FullChainAssetId(pahChainId, dotAssetId),
            tokenLocation = dotLocation
        )

        val usdcPahReserve = ChainAssetReserveConfig(
            reserveId = "USDC-PAH",
            reserveAssetId = FullChainAssetId(pahChainId, usdcAssetId),
            tokenLocation = usdcLocation
        )

        val reservesById = mapOf(
            "DOT" to dotReserve,
            "DOT-PAH" to dotPahReserve,
            "USDC-PAH" to usdcPahReserve
        )

        val assetToReserveIdOverrides = mapOf(
            FullChainAssetId(pahChainId, dotAssetId) to "DOT-PAH",
            FullChainAssetId(pahChainId, usdcAssetId) to "USDC-PAH"
        )

        whenever(xcmConfig.reservesById).thenReturn(reservesById)
        whenever(xcmConfig.assetToReserveIdOverrides).thenReturn(assetToReserveIdOverrides)
    }
}
