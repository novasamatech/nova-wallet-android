package io.novafoundation.nova.common.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KotlinExtTest {

    @Test
    fun `endsWith should return false for suffix bigger than content`() {
        assertFalse(byteArrayOf(0, 1).endsWith(byteArrayOf(0, 1, 2)))
    }

    @Test
    fun `endsWith should return true for suffix equal to the content`() {
        assertTrue(byteArrayOf(0, 1, 2).endsWith(byteArrayOf(0, 1, 2)))
    }

    @Test
    fun `endsWith should return true for correct suffix`() {
        assertTrue(byteArrayOf(0, 1, 2).endsWith(byteArrayOf(1, 2)))
    }

    @Test
    fun `endsWith should return true for incorrect suffix`() {
        assertFalse(byteArrayOf(0, 1, 2, 3).endsWith(byteArrayOf(1, 2)))
        assertFalse(byteArrayOf(0, 1, 2, 3).endsWith(byteArrayOf(2)))
    }
}
