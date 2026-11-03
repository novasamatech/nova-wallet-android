package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

class BittensorPositionInvalidationTest {

    private val positions = mock(BittensorPositionSource::class.java)

    private val prices = mock(BittensorPricesSource::class.java)

    private val loader = mock(BittensorPortfolioLoader::class.java)

    private val repository = RealBittensorPositionRepository(positions, loader, prices)

    private val coldKey = ByteArray(32)

    @Test
    fun `success invalidates both sources`() = runBlocking {
        assertTrue(repository.invalidate("chain", coldKey, 1L).isSuccess)
        verify(positions).invalidate("chain", coldKey, 1L)
        verify(prices).invalidate("chain", coldKey, 1L)
    }

    @Test
    fun `position failure is returned after prices are invalidated`() = runBlocking {
        val failure = IllegalStateException("Positions unavailable")
        doThrow(failure).`when`(positions).invalidate("chain", coldKey, 1L)

        val result = repository.invalidate("chain", coldKey, 1L)

        assertSame(failure, result.exceptionOrNull())
        verify(prices).invalidate("chain", coldKey, 1L)
    }

    @Test
    fun `price failure is returned to the caller`() = runBlocking {
        val failure = IllegalStateException("Prices unavailable")
        doThrow(failure).`when`(prices).invalidate("chain", coldKey, 1L)

        val result = repository.invalidate("chain", coldKey, 1L)

        assertSame(failure, result.exceptionOrNull())
        verify(positions).invalidate("chain", coldKey, 1L)
    }

    @Test
    fun `two failures preserve the first cause and the second as suppressed`() = runBlocking {
        val positionsFailure = IllegalStateException("Positions unavailable")
        val pricesFailure = IllegalStateException("Prices unavailable")
        doThrow(positionsFailure).`when`(positions).invalidate("chain", coldKey, 1L)
        doThrow(pricesFailure).`when`(prices).invalidate("chain", coldKey, 1L)

        val result = repository.invalidate("chain", coldKey, 1L)

        assertSame(positionsFailure, result.exceptionOrNull())
        assertEquals(listOf(pricesFailure), positionsFailure.suppressed.toList())
    }

    @Test
    fun `cancellation stops subsequent work and is not returned as failure`() = runBlocking {
        val cancellation = CancellationException("Leaving")
        doThrow(cancellation).`when`(positions).invalidate("chain", coldKey, 1L)

        try {
            repository.invalidate("chain", coldKey, 1L)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        verifyNoInteractions(prices)
    }

    @Test
    fun `price cancellation is not returned as failure`() = runBlocking {
        val cancellation = CancellationException("Leaving")
        doThrow(cancellation).`when`(prices).invalidate("chain", coldKey, 1L)

        try {
            repository.invalidate("chain", coldKey, 1L)
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
        verify(positions).invalidate("chain", coldKey, 1L)
    }
}
