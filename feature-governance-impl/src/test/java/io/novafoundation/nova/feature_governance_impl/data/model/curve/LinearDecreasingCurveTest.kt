package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.LinearDecreasingCurve
import org.junit.Test

class LinearDecreasingCurveTest {

    val curve = LinearDecreasingCurve(
        ceil = 90.percent,
        floor = 10.percent,
        length = 50.percent,
    )

    // x to y
    private val THRESHOLD_TESTS = listOf(
        0.percent to 90.percent,
        25.percent to 50.percent,
        50.percent to 10.percent,
        100.percent to 10.percent
    )

    // y to x
    private val DELAY_TESTS = listOf(
        100.percent to 0.percent,
        90.percent to 0.percent,
        50.percent to 25.percent,
        10.percent to 50.percent,
        9.percent to 100.percent,
        0.percent to 100.percent
    )

    @Test
    fun threshold() {
        curve.runThresholdTests(THRESHOLD_TESTS)
    }

    @Test
    fun delay() {
        curve.runDelayTests(DELAY_TESTS)
    }
}
