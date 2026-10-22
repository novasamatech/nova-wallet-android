package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.BittensorDataEndpoint
import io.novafoundation.nova.analytics.BittensorScreen
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BittensorReadAnalyticsTest {

    private val service = RecordingBittensorAnalytics()

    private val reads = BittensorReadAnalytics(service, BittensorDataEndpoint.VALIDATORS, BittensorScreen.VALIDATOR_SELECT)

    @Test
    fun `repeated failures are reported once until explicit retry`() {
        val error = IOException("private request details")
        reads.failed(error)
        reads.failed(error)
        reads.retry()
        reads.failed(error)

        assertEquals(listOf("bittensor_data_failed", "bittensor_data_retry_tapped", "bittensor_data_failed"), service.events.map { it.name })
        assertEquals("transport", service.events.first().properties["reason"])
        assertFalse(service.events.any { it.properties.toString().contains("private request details") })
    }

    @Test
    fun `new screen has independent failure reporting despite shared service`() {
        val reopened = BittensorReadAnalytics(service, BittensorDataEndpoint.VALIDATORS, BittensorScreen.VALIDATOR_SELECT)
        val error = IOException()
        reads.failed(error)
        reopened.failed(error)

        assertEquals(2, service.events.size)
    }

    @Test
    fun `consent remains controlled by existing service`() {
        service.isEnabled = false
        reads.failed(IOException())
        reads.retry()

        assertEquals(0, service.events.size)
    }
}
