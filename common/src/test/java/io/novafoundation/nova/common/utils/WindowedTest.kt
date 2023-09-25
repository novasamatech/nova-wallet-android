package io.novafoundation.nova.common.utils

import io.novafoundation.nova.test_shared.assertListEquals
import org.junit.Test

class WindowedTest {

    @Test
    fun `window size divides array size`() {
        runTest(
            array = byteArrayOf(1, 2, 3, 4),
            windowSize = 2,
            expected = listOf(
                byteArrayOf(1, 2),
                byteArrayOf(3, 4),
            )
        )
    }

    @Test
    fun `window size does not divide array size`() {
        runTest(
            array = byteArrayOf(1, 2, 3, 4, 5),
            windowSize = 2,
            expected = listOf(
                byteArrayOf(1, 2),
                byteArrayOf(3, 4),
                byteArrayOf(5),
            )
        )
    }

    @Test
    fun `window size 1`() {
        runTest(
            array = byteArrayOf(1, 2, 3),
            windowSize = 1,
            expected = listOf(
                byteArrayOf(1),
                byteArrayOf(2),
                byteArrayOf(3)
            )
        )
    }

    @Test
    fun `input size smaller than window`() {
        runTest(
            array = byteArrayOf(1, 2),
            windowSize = 3,
            expected = listOf(
                byteArrayOf(1, 2)
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `window size is zero`() {
        byteArrayOf().windowed(0)
    }

    private fun runTest(
        array: ByteArray,
        windowSize: Int,
        expected: List<ByteArray>
    ) {
        val result = array.windowed(windowSize)

        assertListEquals(expected, result) { a, b -> a.contentEquals(b) }
    }
}
