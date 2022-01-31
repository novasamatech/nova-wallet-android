package io.novafoundation.nova.runtime.multiNetwork

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Test

class ChainGradientParserTest {

    private val tests = listOf(
        "linear-gradient(135.7deg, #F2A007 19.29%, #A56B00 100.0%)" to
            Chain.Gradient(
                angle = 135.7f,
                colors = listOf("#F2A007", "#A56B00"),
                positions = listOf("19.29".toFloat(), "100".toFloat())
            )
    )

    @Test
    fun `should parse`() {
        tests.forEach { (encoded, decoded) ->
            val decodedActual = ChainGradientParser.parse(encoded)

            assertEquals(decoded, decodedActual)
        }
    }

    @Test
    fun `should encode`() {
        tests.forEach { (encoded, decoded) ->
            val encodedActual = ChainGradientParser.encode(decoded)

            assertEquals(encoded, encodedActual)
        }
    }
}
