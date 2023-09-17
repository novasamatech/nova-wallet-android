package io.novafoundation.nova.common.utils.formatting

import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment


class SpannableFormatterTest {

    @Test
    fun `format not numeric arguments`() {
        val builder = getBuilder("Hello, %s! This is a test: %s")
        val result = SpannableFormatter.fill(builder, "Alice", "123")
        val expected = "Hello, Alice! This is a test: 123"

        assertEquals(expected, result.toString())
    }

    @Test
    fun `format all numeric arguments`() {
        val builder = getBuilder("Hello, %1\$s! This is a test: %2\$s")
        val result = SpannableFormatter.fill(builder, "Alice", "123")
        val expected = "Hello, Alice! This is a test: 123"

        assertEquals(expected, result.toString())
    }

    @Test
    fun `format all numeric arguments reversed`() {
        val builder = getBuilder("Hello, %2\$s! This is a test: %1\$s")
        val result = SpannableFormatter.fill(builder, "123", "Alice")
        val expected = "Hello, Alice! This is a test: 123"

        assertEquals(expected, result.toString())
    }

    @Test
    fun `format numeric and not numeric arguments`() {
        val builder = getBuilder("Hello, %1\$s! This is a test: %s")
        val result = SpannableFormatter.fill(builder, "Alice", "123")
        val expected = "Hello, Alice! This is a test: 123"

        assertEquals(expected, result.toString())
    }

    private fun getBuilder(format: CharSequence): SpannableFormatter.Builder {
        return StubFormatterBuilder(format)
    }
}

class StubFormatterBuilder(override val format: CharSequence) : SpannableFormatter.Builder {

    private var result = format

    override fun replace(start: Int, end: Int, text: CharSequence) {
        result = result.replaceRange(start, end, text)
    }

    override fun result(): CharSequence {
        return result
    }
}
