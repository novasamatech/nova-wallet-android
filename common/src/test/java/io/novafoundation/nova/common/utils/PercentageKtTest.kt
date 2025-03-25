package io.novafoundation.nova.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger

class BigIntegerSplitByWeightsTest {

    @Test
    fun `test empty weights`() {
        val total = BigInteger("100")
        val weights = emptyList<BigInteger>()
        val parts = total.splitByWeights(weights)

        // Since weights are empty, expect an empty list.
        assertTrue(parts.isEmpty())
    }

    @Test
    fun `test negative total`() {
        val total = BigInteger("-10")
        val weights = listOf(BigInteger("2"), BigInteger("5"))
        val parts = total.splitByWeights(weights)

        // Because total is negative, we return zeros (same size as weights).
        assertEquals(listOf(BigInteger.ZERO, BigInteger.ZERO), parts)
    }

    @Test
    fun `test any negative weight`() {
        val total = BigInteger("10")
        val weights = listOf(BigInteger("2"), BigInteger("-1"))
        val parts = total.splitByWeights(weights)

        // Because there's a negative weight, we return zeros.
        assertEquals(listOf(BigInteger.ZERO, BigInteger.ZERO), parts)
    }

    @Test
    fun `test sum of weights is zero`() {
        val total = BigInteger("10")
        val weights = listOf(BigInteger.ZERO, BigInteger.ZERO)
        val parts = total.splitByWeights(weights)

        // Because sumOfWeights == 0, we return zeros.
        assertEquals(listOf(BigInteger.ZERO, BigInteger.ZERO), parts)
    }

    @Test
    fun `test normal distribution`() {
        val total = BigInteger("10")
        val weights = listOf(BigInteger.ONE, BigInteger("2"), BigInteger("3"))
        val parts = total.splitByWeights(weights)

        // sumOfWeights = 6
        // Weighted splits: (1*10)/6 = 1 remainder 4, (2*10)/6 = 3 remainder 2, (3*10)/6 = 5 remainder 0.
        // leftover = total - (1+3+5) = 1
        // We allocate that leftover to the largest remainder => first part => final: [2,3,5]
        val expected = listOf(BigInteger("2"), BigInteger("3"), BigInteger("5"))
        assertEquals(expected, parts)
    }

    @Test
    fun `test all equal weights`() {
        val total = BigInteger("10")
        val weights = (0 until 5).map { BigInteger.ONE }
        val parts = total.splitByWeights(weights)
        val expected = (0 until 5).map { BigInteger.TWO }

        assertEquals(expected, parts)
    }

    @Test
    fun `test weights larger than total`() {
        val total = BigInteger("10")
        val weights = listOf(BigInteger("500"), BigInteger("500"))
        val parts = total.splitByWeights(weights)
        val expected = listOf(BigInteger("5"), BigInteger("5"))

        assertEquals(expected, parts)
    }
}
