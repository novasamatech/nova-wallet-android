package io.novafoundation.nova.feature_governance_impl.data.model.curve

import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

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
            factor = BigInteger.TEN,
            xOffset = BigInteger.ONE,
            yOffset = (-1).toBigInteger()
        )

        curve.runTests(TESTS)
    }
}
