package io.novafoundation.nova.feature_governance_impl.data.model.curve

import io.novafoundation.nova.common.data.network.runtime.binding.Perbill
import io.novafoundation.nova.common.utils.hasTheSaveValueAs
import io.novafoundation.nova.common.utils.percentageToFraction
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VotingCurve
import org.junit.Assert
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext

val Int.percent
    get() = this.toBigDecimal().percentageToFraction()

fun VotingCurve.runThresholdTests(tests: List<Pair<Perbill, Perbill>>) {
    tests.forEach { (x, expectedY) ->
        val y = threshold(x)
        Assert.assertTrue("Expected: ${expectedY}, got: ${y}", expectedY hasTheSaveValueAs y)
    }
}

fun VotingCurve.runDelayTests(tests: List<Pair<Perbill, Perbill>>) {
    tests.forEach { (x, expectedY) ->
        val y = delay(x)
        Assert.assertTrue("Expected $expectedY for input $x but got: $y", expectedY hasTheSaveValueAs y)
    }
}
