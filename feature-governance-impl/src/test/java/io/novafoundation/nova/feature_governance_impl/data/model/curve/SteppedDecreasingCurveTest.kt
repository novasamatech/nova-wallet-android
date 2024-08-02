package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.common.utils.lessEpsilon
import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.LinearDecreasingCurve
import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.SteppedDecreasingCurve
import java.math.BigDecimal
import org.junit.Test

class SteppedDecreasingCurveTest {

    val curve = SteppedDecreasingCurve(
        begin = 80.percent,
        end = 30.percent,
        step = 10.percent,
        period = 15.percent
    )

    // x to y
    private val TESTS = listOf(
        0.percent to 80.percent,
        15.percent.lessEpsilon() to 80.percent,
        15.percent to 70.percent,
        30.percent.lessEpsilon() to 70.percent,
        30.percent to 60.percent,
        100.percent to 30.percent
    )

    // y to x
    private val DELAY_TESTS = listOf(
        80.percent to 0.percent,
        70.percent to 15.percent,
        60.percent to 30.percent,
        30.percent to 75.percent,
        10.percent to 100.percent
    )

    @Test
    fun threshold() {
        curve.runThresholdTests(TESTS)
    }

    @Test
    fun delay() {
        curve.runDelayTests(DELAY_TESTS)
    }
}
