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
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.test_shared.any
import io.novafoundation.nova.test_shared.whenever
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var kusama: Chain

    @Mock
    private lateinit var dotOnPolkadot: Chain.Asset

    @Mock
    private lateinit var dotOnPah: Chain.Asset

    @Mock
    private lateinit var ksmOnKusama: Chain.Asset

    @Mock
    private lateinit var usdcOnPah: Chain.Asset

    @Mock
    private lateinit var ksmtOnPah: Chain.Asset

    @Mock
    private lateinit var chainRegistry: ChainRegistry

    private lateinit var converter: RealChainAssetLocationConverter

    // Chain IDs from real config
    private val polkadotChainId = Chain.Geneses.POLKADOT
    private val pahChainId = Chain.Geneses.POLKADOT_ASSET_HUB

    private val kusamaChainId = Chain.Geneses.KUSAMA

    // Asset IDs
    private val dotAssetId = 0
    private val usdcAssetId = 1337
    private val ksmOnKusamaAssetId = 0
    private val ksmOnPahAssetId = 23


    // Locations
    private val dotLocation = AbsoluteMultiLocation(Interior.Here)

    private val polkadotLocation = AbsoluteMultiLocation(Interior.Here)
    private val pahLocation = AbsoluteMultiLocation(ParachainId(1000))

    private val ksmLocation = AbsoluteMultiLocation(Interior.Here)
    private val kusamaLocation = AbsoluteMultiLocation(Interior.Here)

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

        converter = RealChainAssetLocationConverter(xcmConfig, chainLocationConverter, chainRegistry)
    }

    @Test
    fun `chainAssetFromRelativeLocation should find DOT asset on Polkadot when location points to Polkadot`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 0, interior = Interior.Here)

        val result = converter.chainAssetFromRelativeLocation(relativeLocation, polkadot)

        assertEquals(dotOnPolkadot, result)
    }

    @Test
    fun `chainAssetFromRelativeLocation should find DOT asset on PAH when location points to PAH`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 1, interior = Interior.Here)

        val result = converter.chainAssetFromRelativeLocation(relativeLocation, pah)

        assertEquals(dotOnPah, result)
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
        assertEquals(usdcOnPah, result)
    }

    @Test
    fun `should resolve cross-consensus asset symbol collisions`() = runBlocking {
        // KSM from Kusama PoV
        val relativeLocation = RelativeMultiLocation(parents = 0, interior = Interior.Here)

        val result = converter.chainAssetFromRelativeLocation(relativeLocation, pointOfView = kusama)
        assertEquals(ksmOnKusama, result)
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
        val result = converter.absoluteLocationFromChainAsset(dotOnPolkadot)

        assertEquals(dotLocation, result)
    }

    @Test
    fun `absoluteLocationFromChainAsset should return PAH location for DOT on PAH`() = runBlocking {
        val result = converter.absoluteLocationFromChainAsset(dotOnPah)

        assertEquals(dotLocation, result)
    }

    @Test
    fun `absoluteLocationFromChainAsset should return complex location for USDC on PAH`() = runBlocking {
        val result = converter.absoluteLocationFromChainAsset(usdcOnPah)

        assertEquals(usdcLocation, result)
    }

    @Test
    fun `relativeLocationFromChainAsset should return relative location for DOT on Polkadot from PAH POV`() = runBlocking {
        val result = converter.relativeLocationFromChainAsset(dotOnPolkadot)

        val expectedRelativeLocation = RelativeMultiLocation(parents = 0, interior = Interior.Here)
        assertEquals(expectedRelativeLocation, result)
    }

    @Test
    fun `relativeLocationFromChainAsset should return relative location for DOT on PAH from Polkadot POV`() = runBlocking {
        val result = converter.relativeLocationFromChainAsset(dotOnPah)

        val expectedRelativeLocation = RelativeMultiLocation(parents = 1, interior = Interior.Here)
        assertEquals(expectedRelativeLocation, result)
    }

    @Test
    fun `relativeLocationFromChainAsset should return relative location for USDC with complex location`() = runBlocking {
        val result = converter.relativeLocationFromChainAsset(usdcOnPah)

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
        setupChain(
            chain = polkadot,
            chainId = polkadotChainId,
            assets = mapOf(dotAssetId to dotOnPolkadot),
        )
        setupChain(
            chain = pah,
            chainId = pahChainId,
            assets = mapOf(
                dotAssetId to dotOnPah,
                usdcAssetId to usdcOnPah,
                ksmOnPahAssetId to ksmtOnPah
            )
        )

        setupChain(
            chain = kusama,
            chainId = kusamaChainId,
            assets = mapOf(ksmOnKusamaAssetId to ksmOnKusama),
        )

        whenever(chainRegistry.chainsById).thenAnswer { flowOf(allChainsById()) }
    }

    private fun allChainsById(): Map<ChainId, Chain> {
        return listOf(polkadot, pah, kusama).associateBy { it.id }
    }

    private fun setupAssets() {
        setupAsset(dotOnPolkadot, polkadotChainId, dotAssetId, "DOT")
        setupAsset(dotOnPah, pahChainId, dotAssetId, "DOT")
        setupAsset(usdcOnPah, pahChainId, usdcAssetId, "USDC")
        setupAsset(ksmOnKusama, kusamaChainId, ksmOnKusamaAssetId, "KSM")
        setupAsset(ksmtOnPah, pahChainId, ksmOnPahAssetId, "KSM")
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
        whenever(chainLocationConverter.absoluteLocationFromChain(kusamaChainId)).thenReturn(kusamaLocation)
        whenever(chainLocationConverter.getConsensusRoot(any())).thenAnswer {
            val chain = it.arguments[0] as Chain

            when (chain.id) {
                in listOf(polkadotChainId, pahChainId) -> polkadot
                kusamaChainId -> kusama
                else -> error("Unsupported chain for getConsensusRoot mock")
            }
        }
    }

    private fun setupAssetSearchBySymbol() {
        whenever(polkadot.assets).thenReturn(listOf(dotOnPolkadot))
        whenever(pah.assets).thenReturn(listOf(dotOnPah, usdcOnPah))
        whenever(kusama.assets).thenReturn(listOf(ksmOnKusama))
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

        val ksmReserve = ChainAssetReserveConfig(
            reserveId = "KSM",
            reserveAssetId = FullChainAssetId(kusamaChainId, ksmOnKusamaAssetId),
            tokenLocation = kusamaLocation
        )

        val reservesById = mapOf(
            "DOT" to dotReserve,
            "DOT-PAH" to dotPahReserve,
            "USDC-PAH" to usdcPahReserve,
            "KSM" to ksmReserve
        )

        val assetToReserveIdOverrides = mapOf(
            FullChainAssetId(pahChainId, dotAssetId) to "DOT-PAH",
            FullChainAssetId(pahChainId, usdcAssetId) to "USDC-PAH"
        )

        whenever(xcmConfig.reservesById).thenReturn(reservesById)
        whenever(xcmConfig.assetToReserveIdOverrides).thenReturn(assetToReserveIdOverrides)
    }
}
