package io.novafoundation.nova.feature_governance_impl.data.model.curve

import org.junit.Test

class LinearDecreasingCurveTest {

    private val TESTS = listOf(
        0.percent to 90.percent,
        25.percent to 50.percent,
        50.percent to 10.percent,
        100.percent to 10.percent
    )

    @Test
    fun threshold() {
        val curve = LinearDecreasingCurve(
            ceil = 90.percent,
            floor = 10.percent,
            length = 50.percent,
        )

        curve.runTests(TESTS)
    }
}
