package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.feature_xcm_api.config.model.ChainXcmConfig
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.GlobalConsensus
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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

@RunWith(MockitoJUnitRunner::class)
class RealChainLocationConverterTest {

    @Mock
    private lateinit var xcmConfig: ChainXcmConfig

    @Mock
    private lateinit var chainRegistry: ChainRegistry

    @Mock
    private lateinit var polkadot: Chain
    @Mock
    private lateinit var pah: Chain

    @Mock
    private lateinit var kusama: Chain
    @Mock
    private lateinit var kah: Chain


    private lateinit var converter: RealChainLocationConverter

    private val polkadotId = Chain.Geneses.POLKADOT
    private val pahId = Chain.Geneses.POLKADOT_ASSET_HUB

    private val kusamaId = Chain.Geneses.KUSAMA
    private val kahId = Chain.Geneses.KUSAMA_ASSET_HUB

    private val paraId = 1000.toBigInteger()

    @Before
    fun setUp() = runBlocking {
        whenever(xcmConfig.parachainIds).thenReturn(mapOf(
            // In difference consensuses, same para id can be used
            pahId to paraId,
            kahId to paraId
        ))

        setupRelay(polkadot, polkadotId)
        setupChain(pah, pahId, parentId = polkadotId)

        setupRelay(kusama, kusamaId)
        setupChain(kah, kahId, parentId = kusamaId)

        whenever(chainRegistry.chainsById).thenAnswer { flowOf(allChainsById()) }

        converter = RealChainLocationConverter(xcmConfig, chainRegistry)
    }

    private fun allChainsById(): Map<ChainId, Chain> {
        return listOf(polkadot, pah, kusama, kah).associateBy { it.id  }
    }

    private suspend fun setupRelay(relaychain: Chain, relayId: String) {
        setupChain(relaychain, relayId, parentId = null)
    }

    private suspend fun setupChain(relaychain: Chain, chainId: String, parentId: String?) {
        whenever(relaychain.id).thenReturn(chainId)
        whenever(relaychain.parentId).thenReturn(parentId)
        whenever(chainRegistry.getChain(chainId)).thenReturn(relaychain)
    }

    @Test
    fun `chainFromRelativeLocation should return relaychain when location points to relaychain`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 1, interior = Interior.Here)

        val result = converter.chainFromRelativeLocation(relativeLocation, pah)

        assertEquals(polkadot, result)
    }

    @Test
    fun `chainFromRelativeLocation should return parachain when location points to parachain`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 0, interior = Junctions(ParachainId(paraId)))

        val result = converter.chainFromRelativeLocation(relativeLocation, polkadot)

        assertEquals(pah, result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return relaychain when location has no junctions`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(GlobalConsensus(polkadotId))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation)

        assertEquals(polkadot, result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return parachain when location has parachain junction`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(GlobalConsensus(polkadotId), ParachainId(paraId))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation)

        assertEquals(pah, result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return null when parachain not found`() = runBlocking {
        val unknownParaId = 9999
        val absoluteLocation = AbsoluteMultiLocation(GlobalConsensus(polkadotId), ParachainId(unknownParaId))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation)

        assertNull(result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return null when global consensus is missing`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(ParachainId(paraId))
        val result = converter.chainFromAbsoluteLocation(absoluteLocation)
        assertNull(result)
    }

    @Test
    fun `absoluteLocationFromChain should return global consensus for relay`() = runBlocking {
        val expected = AbsoluteMultiLocation(GlobalConsensus(polkadotId))

        val result = converter.absoluteLocationFromChain(polkadot)

        assertEquals(expected, result)
    }

    @Test
    fun `absoluteLocationFromChain should return parachain location for parachain`() = runBlocking {
        val expected = AbsoluteMultiLocation(GlobalConsensus(polkadotId), ParachainId(paraId))

        val result = converter.absoluteLocationFromChain(pah)

        assertEquals(expected, result)
    }
}
