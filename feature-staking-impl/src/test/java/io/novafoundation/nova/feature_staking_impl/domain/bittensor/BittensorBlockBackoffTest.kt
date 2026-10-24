package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorBlockBackoffTest {

    @Test
    fun `retries back off exponentially and stop after the limit`() {
        val backoff = BittensorBlockBackoff(maxAttempts = 3)
        val attempts = (1..40).filter { backoff.shouldAttempt() }
        assertEquals(listOf(1, 3, 7), attempts)
    }
}
