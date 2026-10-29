package io.novafoundation.nova.runtime.call

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class RuntimeCallsApiTest {

    private val runtimeCalls = mock(RuntimeCallsApi::class.java)

    private val arguments = mapOf<String, Any?>("netuid" to 1.toBigInteger())

    private val binding: (Any?) -> String = { it as String }

    @Test
    fun `runtime call failure retains its cause`() = runBlocking {
        val failure = IllegalStateException("Unavailable runtime call")
        `when`(runtimeCalls.call("Api", "method", arguments, binding)).thenThrow(failure)

        val result = runtimeCalls.callCatching("Api", "method", arguments, binding)

        assertSame(failure, result.exceptionOrNull())
    }

    @Test
    fun `runtime call cancellation propagates`() = runBlocking {
        val cancellation = CancellationException("Caller cancelled")
        `when`(runtimeCalls.call("Api", "method", arguments, binding)).thenThrow(cancellation)

        try {
            runtimeCalls.callCatching("Api", "method", arguments, binding)
            fail("Cancellation must propagate")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }

    @Test
    fun `runtime call result is retained`() = runBlocking {
        `when`(runtimeCalls.call("Api", "method", arguments, binding)).thenReturn("decoded")

        assertEquals("decoded", runtimeCalls.callCatching("Api", "method", arguments, binding).getOrThrow())
    }
}
