package io.novafoundation.nova.feature_governance_impl.data.model.curve

import org.junit.Test
import java.math.BigDecimal

class ReciprocalCurveTest {

    private val TESTS = listOf(
        BigDecimal.ZERO to 11.toBigDecimal(),
        BigDecimal.ONE to 6.toBigDecimal(),
        3.toBigDecimal() to 3.5.toBigDecimal()
    )

    @Test
    fun threshold() {
        // 10/(x + 1) + 1
        val curve = ReciprocalCurve(
            factor = BigDecimal.TEN,
            xOffset = BigDecimal.ONE,
            yOffset = (-1).toBigDecimal()
        )

        curve.runTests(TESTS)
    }
}
