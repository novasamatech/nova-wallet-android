package io.novafoundation.nova.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

internal class PartitionTest {

    @Test
    fun testEveryCombinationBelowSize100() {
        (1..100).map {  size ->
            (0 .. size).map { truePointIndex ->
                val list = List(truePointIndex) { false } + List(size - truePointIndex) { true }
                runTest(list, expectedResult = truePointIndex.takeIf { truePointIndex < size })
            }
        }
    }

    private fun runTest(
        list: List<Boolean>,
        expectedResult: Int?
    ) {
        var iterationCount = 0
        val actualResult = list.findPartitionPoint { iterationCount++; it }

        assertEquals("Expected: ${expectedResult}, Got: $actualResult in $list", expectedResult, actualResult)
    }
}
