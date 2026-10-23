package io.novafoundation.nova.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BidiExtTest {

    @Test
    fun `wraps text into a left-to-right isolate`() {
        assertEquals("⁦Max: 70.2 ش⁩", "Max: 70.2 ش".ltrIsolated())
        assertEquals("⁦⁩", "".ltrIsolated())
    }

    @Test
    fun `char sequence overload isolates plain strings the same way`() {
        val text: CharSequence = "ش pool"

        assertEquals("⁦ش pool⁩", text.ltrIsolated())
    }

    @Test
    fun `isolates both sides of a rate and the whole rate`() {
        assertEquals(
            "⁦⁦1 ش⁩ ≈ ⁦0.067 TAO⁩⁩",
            ltrIsolatedRate("1 ش", "0.067 TAO")
        )
    }
}
