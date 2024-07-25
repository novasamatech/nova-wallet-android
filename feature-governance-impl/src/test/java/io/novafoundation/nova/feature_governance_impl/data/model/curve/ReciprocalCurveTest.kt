package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.ReciprocalCurve
import org.junit.Test
import java.math.BigDecimal

class ReciprocalCurveTest {

    // 10/(x + 1) - 1
    val curve = ReciprocalCurve(
        factor = BigDecimal.TEN,
        xOffset = BigDecimal.ONE,
        yOffset = (-1).toBigDecimal()
    )

    // x to y
    private val TESTS = listOf(
        BigDecimal.ZERO to 9.toBigDecimal(),
        0.25.toBigDecimal() to 7.toBigDecimal(),
        BigDecimal.ONE to 4.toBigDecimal(),
        3.toBigDecimal() to 1.5.toBigDecimal()
    )

    // y to x
    private val DELAY_TESTS = listOf(
        9.toBigDecimal() to BigDecimal.ZERO,
        7.toBigDecimal() to 0.25.toBigDecimal(),
        4.toBigDecimal() to BigDecimal.ONE
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
