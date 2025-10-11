package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import org.junit.Assert.assertEquals
import org.junit.Test


class RelativeMultiLocationTest {

    @Test
    fun `reanchor global pov should remain unchanged`() {
        val expected = AbsoluteMultiLocation(ParachainId(1000))
        val pov = AbsoluteMultiLocation(Interior.Here)

        val relative = RelativeMultiLocation(
            parents = 0,
            interior = Junctions(ParachainId(1000))
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }

    @Test
    fun `reanchor no common junctions`() {
        val expected = AbsoluteMultiLocation(ParachainId(1000))
        val pov = AbsoluteMultiLocation(ParachainId(2000))

        val relative = RelativeMultiLocation(
            parents = 1,
            interior = Junctions(ParachainId(1000))
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }

    @Test
    fun `reanchor one common junction`() {
        val expected = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val pov = AbsoluteMultiLocation(ParachainId(1000), ParachainId(3000))

        val relative = RelativeMultiLocation(
            parents = 1,
            interior = Junctions(ParachainId(2000))
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }

    @Test
    fun `reanchor all common junction`() {
        val expected = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val pov = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))

        val relative = RelativeMultiLocation(
            parents = 0,
            interior = Interior.Here
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }

    @Test
    fun `reanchor global to global`() {
        val expected = AbsoluteMultiLocation(Interior.Here)
        val pov = AbsoluteMultiLocation(Interior.Here)

        val relative = RelativeMultiLocation(
            parents = 0,
            interior = Interior.Here
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }

    @Test
    fun `reanchor pov is successor of initial`() {
        val expected = AbsoluteMultiLocation(Interior.Here)
        val pov = AbsoluteMultiLocation(ParachainId(1000))

        // Target is ancestor of POV: go up 1, then Here
        val relative = RelativeMultiLocation(
            parents = 1,
            interior = Interior.Here
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }

    @Test
    fun `reanchor initial is successor of pov`() {
        val expected = AbsoluteMultiLocation(ParachainId(1000), ParachainId(2000))
        val pov = AbsoluteMultiLocation(ParachainId(1000))

        val relative = RelativeMultiLocation(
            parents = 0,
            interior = Junctions(ParachainId(2000))
        )

        val restored = relative.absoluteLocationViewingFrom(pov)
        assertEquals(expected, restored)
    }
}
