package io.novafoundation.nova.feature_governance_impl.data.model.curve

import org.junit.Test

class SteppedDecreasingCurveTest {

    private val TESTS = listOf(
        0.percent to 80.percent,
        15.percent.lessEpsilon() to 80.percent,
        15.percent to 70.percent,
        30.percent.lessEpsilon() to 70.percent,
        30.percent to 60.percent,
        100.percent to 30.percent
    )

    @Test
    fun threshold() {
        val curve = SteppedDecreasingCurve(
            begin = 80.percent,
            end = 30.percent,
            step = 10.percent,
            period = 15.percent
        )

        curve.runTests(TESTS)
    }
}
