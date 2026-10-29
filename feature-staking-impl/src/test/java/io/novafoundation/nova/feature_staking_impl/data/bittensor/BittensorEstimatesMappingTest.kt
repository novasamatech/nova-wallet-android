package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BittensorEstimatesMappingTest {

    private val chain = mock(Chain::class.java)

    private val subnetId = BittensorNetuid.Subnet(1)

    @Test
    fun `history mapping preserves provider fields including an absent operation type`() {
        val remote = operation(1)

        val operation = mapBittensorReportedOperation(remote)

        assertEquals(1, operation.netuid)
        assertEquals("hotkey", operation.hotkey)
        assertEquals("2.0", operation.amountIn)
        assertEquals("4.0", operation.amountOut)
        assertEquals("0.5", operation.price)
        assertEquals("event", operation.eventId)
        assertEquals("timestamp", operation.timestamp)
        assertEquals("reference", operation.reference)
        assertNull(operation.type)
    }

    @Test
    fun `netuid validation belongs to the domain type including root`() {
        assertEquals(
            0,
            mapBittensorReportedOperation(operation(0)).netuid
        )
        assertEquals(
            65535,
            mapBittensorReportedOperation(operation(65535)).netuid
        )
        assertTrue(
            runCatching { mapBittensorReportedOperation(operation(-1)) }
                .isFailure
        )
        assertTrue(
            runCatching { mapBittensorReportedOperation(operation(65536)) }
                .isFailure
        )
        assertTrue(
            runCatching { mapBittensorReportedOperation(operation(netuid = null)) }
                .isFailure
        )
    }

    @Test
    fun `invalid yield fields are skipped while zero is a valid rate`() {
        val address = chain.addressOf(ByteArray(32) { 1 })
        val zero = BittensorAlphaYieldsRemote.Item(
            1,
            address,
            "0",
            "ALPHA_VALIDATOR_APY"
        )
        val negative = BittensorAlphaYieldsRemote.Item(
            1,
            address,
            "-1",
            "ALPHA_VALIDATOR_APY"
        )
        val missing = BittensorAlphaYieldsRemote.Item(
            1,
            address,
            reportedRate = null,
            "ALPHA_VALIDATOR_APY"
        )
        val malformed = BittensorAlphaYieldsRemote.Item(
            1,
            address,
            "unknown",
            "ALPHA_VALIDATOR_APY"
        )

        assertEquals(
            BigDecimal.ZERO,
            mapBittensorValidatorYield(zero, chain, subnetId)
                ?.annualPercentageYield
        )
        assertNull(mapBittensorValidatorYield(negative, chain, subnetId))
        assertNull(mapBittensorValidatorYield(missing, chain, subnetId))
        assertNull(mapBittensorValidatorYield(malformed, chain, subnetId))
    }

    @Test
    fun `only a RELIABLE verdict or an absent one permits recommendations`() {
        val address = chain.addressOf(ByteArray(32) { 1 })
        fun item(rate: String, verdict: String?) = BittensorAlphaYieldsRemote.Item(
            1,
            address,
            rate,
            "ALPHA_VALIDATOR_APY",
            rateReliability = verdict
        )

        // A dust nominated stake compounded into 669,990 percent: the backend verdict hides it.
        assertEquals(false, mapBittensorValidatorYield(item("669990.45", "UNRELIABLE"), chain, subnetId)?.reliable)
        assertEquals(false, mapBittensorValidatorYield(item("79.56", "MAYBE"), chain, subnetId)?.reliable)
        assertEquals(
            BigDecimal("79.56"),
            mapBittensorValidatorYield(item("79.56", "RELIABLE"), chain, subnetId)?.annualPercentageYield
        )
        assertEquals(
            BigDecimal("79.56"),
            mapBittensorValidatorYield(item("79.56", verdict = null), chain, subnetId)?.annualPercentageYield
        )
    }

    @Test
    fun `address decoding cancellation is propagated instead of skipping the yield`() {
        val cancellation = CancellationException("Cancelled")
        `when`(chain.isEthereumBased).thenThrow(cancellation)
        val remote = BittensorAlphaYieldsRemote.Item(1, "hotkey", "12.5", "ALPHA_VALIDATOR_APY")

        val failure = runCatching { mapBittensorValidatorYield(remote, chain, subnetId) }.exceptionOrNull()

        assertSame(cancellation, failure)
    }

    @Test
    fun `address decoding errors are propagated instead of skipping the yield`() {
        val error = AssertionError("Decoder failure")
        `when`(chain.isEthereumBased).thenThrow(error)
        val remote = BittensorAlphaYieldsRemote.Item(1, "hotkey", "12.5", "ALPHA_VALIDATOR_APY")

        val failure = runCatching { mapBittensorValidatorYield(remote, chain, subnetId) }.exceptionOrNull()

        assertSame(error, failure)
    }

    @Test
    fun `missing and malformed hotkeys are skipped`() {
        for (hotkey in listOf(null, "", "invalid")) {
            val remote = BittensorAlphaYieldsRemote.Item(1, hotkey, "12.5", "ALPHA_VALIDATOR_APY")

            assertNull(mapBittensorValidatorYield(remote, chain, subnetId))
        }
    }

    @Test
    fun `unexpected subnet or metric is rejected even when the yield is unusable`() {
        val wrongSubnet = BittensorAlphaYieldsRemote.Item(2, hotkey = null, reportedRate = null, "ALPHA_VALIDATOR_APY")
        val missingSubnet = BittensorAlphaYieldsRemote.Item(netuid = null, hotkey = null, reportedRate = null, "ALPHA_VALIDATOR_APY")
        val wrongMetric = BittensorAlphaYieldsRemote.Item(1, hotkey = null, reportedRate = null, "OTHER")
        val missingMetric = BittensorAlphaYieldsRemote.Item(1, hotkey = null, reportedRate = null, metricKind = null)

        for (remote in listOf(wrongSubnet, missingSubnet, wrongMetric, missingMetric)) {
            val failure = runCatching { mapBittensorValidatorYield(remote, chain, subnetId) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
        }
    }

    @Test
    fun `fractional and large yields preserve annual percent units`() {
        val address = chain.addressOf(ByteArray(32) { 1 })
        for (rate in listOf("0.125", "12.5", "669990.45")) {
            val remote = BittensorAlphaYieldsRemote.Item(1, address, rate, "ALPHA_VALIDATOR_APY")

            assertEquals(BigDecimal(rate), mapBittensorValidatorYield(remote, chain, subnetId)?.annualPercentageYield)
        }
    }

    private fun operation(netuid: Int?) = BittensorOperationsRemote.Item(
        netuid = netuid,
        hotkey = "hotkey",
        reportedAmountIn = "2.0",
        reportedAmountOut = "4.0",
        reportedPrice = "0.5",
        sourceEventId = "event",
        sourceTimestamp = "timestamp",
        sourceExtrinsicReference = "reference",
        sourceOperationType = null
    )
}
