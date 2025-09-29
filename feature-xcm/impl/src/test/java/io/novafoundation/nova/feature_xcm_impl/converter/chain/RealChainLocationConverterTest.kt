package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.feature_xcm_api.config.model.ChainXcmConfig
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.Junctions
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
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
class RealChainLocationConverterTest {

    @Mock
    private lateinit var xcmConfig: ChainXcmConfig

    @Mock
    private lateinit var chainRegistry: ChainRegistry

    @Mock
    private lateinit var relayChain: Chain

    @Mock
    private lateinit var parachain: Chain

    private lateinit var converter: RealChainLocationConverter

    private val relayChainId = "polkadot"
    private val parachainId = "asset-hub"
    private val paraId = 1000.toBigInteger()

    @Before
    fun setUp() = runBlocking {
        whenever(xcmConfig.parachainIds).thenReturn(mapOf(parachainId to paraId))

        whenever(relayChain.id).thenReturn(relayChainId)
        whenever(relayChain.parentId).thenReturn(null)

        whenever(parachain.id).thenReturn(parachainId)
        whenever(parachain.parentId).thenReturn(relayChainId)

        whenever(chainRegistry.getChain(relayChainId)).thenReturn(relayChain)
        whenever(chainRegistry.getChain(parachainId)).thenReturn(parachain)

        converter = RealChainLocationConverter(xcmConfig, chainRegistry)
    }

    @Test
    fun `chainFromRelativeLocation should return relaychain when location points to relaychain`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 1, interior = Interior.Here)

        val result = converter.chainFromRelativeLocation(relativeLocation, parachain)

        assertEquals(relayChain, result)
    }

    @Test
    fun `chainFromRelativeLocation should return parachain when location points to parachain`() = runBlocking {
        val relativeLocation = RelativeMultiLocation(parents = 0, interior = Junctions(ParachainId(paraId)))

        val result = converter.chainFromRelativeLocation(relativeLocation, relayChain)

        assertEquals(parachain, result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return relaychain when location has no junctions`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(Interior.Here)

        val result = converter.chainFromAbsoluteLocation(absoluteLocation, relayChain)

        assertEquals(relayChain, result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return parachain when location has parachain junction`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(ParachainId(paraId))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation, relayChain)

        assertEquals(parachain, result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return null when parachain not found`() = runBlocking {
        val unknownParaId = 9999
        val absoluteLocation = AbsoluteMultiLocation(ParachainId(unknownParaId))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation, relayChain)

        assertNull(result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return null when location has multiple junctions`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(ParachainId(paraId), ParachainId(2000))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation, relayChain)

        assertNull(result)
    }

    @Test
    fun `chainFromAbsoluteLocation should return null when junction is not ParachainId`() = runBlocking {
        val absoluteLocation = AbsoluteMultiLocation(MultiLocation.Junction.GeneralIndex(BigInteger.ZERO))

        val result = converter.chainFromAbsoluteLocation(absoluteLocation, relayChain)

        assertNull(result)
    }

    @Test
    fun `absoluteLocationFromChain should return Here location for relaychain`() = runBlocking {
        val expected = Interior.Here.asLocation()

        val result = converter.absoluteLocationFromChain(relayChainId)

        assertEquals(expected, result)
    }

    @Test
    fun `absoluteLocationFromChain should return parachain location for parachain`() = runBlocking {
        val expected = AbsoluteMultiLocation(ParachainId(paraId))

        val result = converter.absoluteLocationFromChain(parachainId)

        assertEquals(expected, result)
    }
}
