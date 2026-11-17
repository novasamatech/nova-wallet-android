package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import io.novasama.substrate_sdk_android.extensions.fromHex
import org.junit.Assert.assertEquals
import org.junit.Test


class AbsoluteMultiLocationTest {

    private val ethereum = Junction.GlobalConsensus(MultiLocation.NetworkId.Ethereum(chainId = 1))
    private val wethContract = Junction.AccountKey20(AccountIdKey("0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2".fromHex()))

    @Test
    fun `reanchor global pov should remain unchanged`() {
        val initial = AbsoluteMultiLocation(ParachainId(1000))
        val pov = AbsoluteMultiLocation(Interior.Here)
        val expected = initial.toRelative()

        val result = initial.fromPointOfViewOf(pov)
        assertEquals(expected, result)
    }

    @Test
    fun `reanchor no common junctions`() {
        val initial = AbsoluteMultiLocation(ParachainId(1000))
        val pov = AbsoluteMultiLocation(ParachainId(2000))
        val expected = RelativeMultiLocation(parents=1, interior = Junctions(ParachainId(1000)))

        val result = initial.fromPointOfViewOf(pov)

        assertEquals(expected, result)
    }

    @Test
    fun `reanchor one common junction`() {
        val initial = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val pov = AbsoluteMultiLocation(ParachainId(1000), ParachainId(3000))
        val expected = RelativeMultiLocation(parents=1, interior = Junctions(ParachainId(2000)))

        val result = initial.fromPointOfViewOf(pov)

        assertEquals(expected, result)
    }

    @Test
    fun `reanchor all common junction`() {
        val initial = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val pov = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val expected = RelativeMultiLocation(parents=0, interior = Interior.Here)

        val result = initial.fromPointOfViewOf(pov)

        assertEquals(expected, result)
    }

    @Test
    fun `reanchor global to global`() {
        val initial = AbsoluteMultiLocation(Interior.Here)
        val pov = AbsoluteMultiLocation(Interior.Here)
        val expected = initial.toRelative()

        val result = initial.fromPointOfViewOf(pov)

        assertEquals(expected, result)
    }

    @Test
    fun `reanchor pov is successor of initial`() {
        val initial = AbsoluteMultiLocation(Interior.Here)
        val pov = AbsoluteMultiLocation(ParachainId(1000))
        val expected = RelativeMultiLocation(parents=1, interior = Interior.Here)

        val result = initial.fromPointOfViewOf(pov)

        assertEquals(expected, result)
    }

    @Test
    fun `reanchor initial is successor of pov`() {
        val initial = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val pov = AbsoluteMultiLocation(ParachainId(1000))
        val expected = RelativeMultiLocation(parents=0, interior = Junctions(ParachainId(2000)))

        val result = initial.fromPointOfViewOf(pov)

        assertEquals(expected, result)
    }

    @Test
    fun `reanchor foreign consensus from parachain`() {
        val initial = AbsoluteMultiLocation(ethereum)
        val pov = AbsoluteMultiLocation(ParachainId(1000))
        val expected = RelativeMultiLocation(parents = 2, interior = Junctions(ethereum))

        assertEquals(expected, initial.fromPointOfViewOf(pov))
    }

    @Test
    fun `reanchor foreign consensus from relay`() {
        val initial = AbsoluteMultiLocation(ethereum)
        val pov = AbsoluteMultiLocation(Interior.Here)
        val expected = RelativeMultiLocation(parents = 1, interior = Junctions(ethereum))

        assertEquals(expected, initial.fromPointOfViewOf(pov))
    }

    @Test
    fun `reanchor foreign consensus erc20 from parachain`() {
        val initial = AbsoluteMultiLocation(ethereum, wethContract)
        val pov = AbsoluteMultiLocation(ParachainId(2034))
        val expected = RelativeMultiLocation(parents = 2, interior = Junctions(ethereum, wethContract))

        assertEquals(expected, initial.fromPointOfViewOf(pov))
    }
}
