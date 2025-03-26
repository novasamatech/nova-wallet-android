package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Interior
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction.ParachainId
import org.junit.Assert.assertEquals
import org.junit.Test


class AbsoluteMultiLocationTest {

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
}
