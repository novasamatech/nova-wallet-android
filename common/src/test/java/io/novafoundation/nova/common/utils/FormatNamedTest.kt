package io.novafoundation.nova.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatNamedTest {

    @Test
    fun `should format one argument`() = runTest(
        template = "https://moonbase.subscan.io/account/{address}",
        values = mapOf(
            "address" to "test"
        ),
        expected = "https://moonbase.subscan.io/account/test"
    )

    @Test
    fun `should format multiple arguments`() = runTest(
        template = "https://moonbase.subscan.io/{a}/{b}/{c}",
        values = mapOf(
            "a" to "A",
            "b" to "B",
            "c" to "C",
        ),
        expected = "https://moonbase.subscan.io/A/B/C"
    )

    @Test
    fun `can use the same argument twice`() = runTest(
        template = "https://moonbase.subscan.io/{a}/{a}",
        values = mapOf(
            "a" to "A",
        ),
        expected = "https://moonbase.subscan.io/A/A"
    )

    @Test
    fun `should format missing value`() = runTest(
        template = "https://moonbase.subscan.io/account/{address}",
        values = emptyMap(),
        expected = "https://moonbase.subscan.io/account/null"
    )

    private fun runTest(template: String, values: Map<String, String>, expected: String) {
        val actual = template.formatNamed(values)

        assertEquals(expected, actual)
    }
}
