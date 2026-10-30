package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class BittensorLoadingValueTest {

    @Test
    fun `pending dependency does not format an unavailable value`() {
        val result = bittensorLoadingValue(ExtendedLoadingState.Loading) {
            error("Pending data must not be formatted")
        }

        assertSame(ExtendedLoadingState.Loading, result)
    }

    @Test
    fun `missing data is settled rather than perpetual loading`() {
        val result = bittensorLoadingValue(ExtendedLoadingState.Loaded(null)) { "—" }

        assertEquals(ExtendedLoadingState.Loaded("—"), result)
    }

    @Test
    fun `failure wins over another pending dependency`() {
        val failure = ExtendedLoadingState.Error(IllegalStateException("Unavailable"))
        val result = bittensorLoadingValue(failure, ExtendedLoadingState.Loading) {
            error("Failed data must not be formatted")
        }

        assertSame(failure, result)
    }

    @Test
    fun `loaded values are formatted including zero`() {
        val result = bittensorLoadingValue(ExtendedLoadingState.Loaded(0)) { "0" }

        assertEquals(ExtendedLoadingState.Loaded("0"), result)
    }
}
