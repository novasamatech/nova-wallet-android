package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.feature_governance_api.data.thresold.gov2.curve.ReciprocalCurve
import org.junit.Test
import java.math.BigDecimal

class ReciprocalCurveTest {

    private val TESTS = listOf(
        BigDecimal.ZERO to 9.toBigDecimal(),
        BigDecimal.ONE to 4.toBigDecimal(),
        3.toBigDecimal() to 1.5.toBigDecimal()
    )

    @Test
    fun threshold() {
        // 10/(x + 1) - 1
        val curve = ReciprocalCurve(
            factor = BigDecimal.TEN,
            xOffset = BigDecimal.ONE,
            yOffset = (-1).toBigDecimal()
        )

        curve.runTests(TESTS)
    }
}
