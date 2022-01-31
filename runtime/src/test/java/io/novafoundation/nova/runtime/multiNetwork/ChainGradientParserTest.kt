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
                positionsPercent = listOf("19.29".toFloat(), "100".toFloat())
            ),
        "linear-gradient(135.0deg, #12D5D5 0.0%, #4584F5 40.32%, #AC57C0 60.21%, #E65659 80.19%, #FFBF12 100.0%)" to
            Chain.Gradient(
                angle = 135f,
                colors = listOf("#12D5D5", "#4584F5", "#AC57C0", "#E65659", "#FFBF12"),
                positionsPercent = listOf(0f, 40.32f, 60.21f, 80.19f, 100f)
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
