package io.novafoundation.nova.feature_account_api.data.mappers

import org.junit.Assert.assertEquals
import org.junit.Test

class AngleConversionsTest {

    private val cssToAndroid = listOf(
        0 to 90,
        45 to 45,
        90 to 0,
        135 to 315,
        180 to 270,
        225 to 225,
        270 to 180,
        315 to 135
    )

    @Test
    fun `should convert css angles to android`() {
        cssToAndroid.forEach { (css, android) ->
            assertEquals(android, cssAngleToAndroid(css))
        }
    }
}
