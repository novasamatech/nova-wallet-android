package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorEstimatedCostBasisTest {

    private val precision = BigInteger.TEN.pow(9)

    @Test fun `purchase and partial sale retain proportional cost then combine a new purchase`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "10",
                    "50",
                    "0.2",
                    "3"
                ),
                operation(
                    "50",
                    "5",
                    "0.1",
                    "2"
                ),
                operation(
                    "10",
                    "100",
                    "0.1",
                    "1"
                )
            ),
            true
        )
        val totals = requireNotNull(ledger.forSubnet(64))
        assertEquals(precision * BigInteger.valueOf(15), totals.paidTao)
        assertEquals(precision * BigInteger.valueOf(100), totals.receivedAlpha)
        assertEquals(
            0,
            BigDecimal("0.15").compareTo(totals.averagePrice)
        )
    }

    @Test fun `ambiguous trade does not produce a confident average`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "1",
                    "1",
                    "1",
                    "2"
                ),
                operation(
                    "10",
                    "100",
                    "0.1",
                    "1"
                )
            ),
            true
        )
        assertFalse(ledger.canEstimate(64))
        assertNull(ledger.forSubnet(64))
    }

    @Test fun `provider pagination cap leaves estimates unavailable`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "10",
                    "100",
                    "0.1",
                    "1"
                )
            ),
            false
        )
        assertFalse(ledger.canEstimate(64))
        assertNull(ledger.forSubnet(64))
    }

    @Test fun `move and transfer camelcase labels do not become purchases`() {
        val buy = operation(
            "10",
            "100",
            "0.1",
            "1"
        )
        val ledger = BittensorEstimatedCostBasis(
            listOf(buy.copy(eventId = "3", type = "transferStake"), buy.copy(eventId = "2", type = "move_stake"), buy),
            true
        )
        assertEquals(
            precision * BigInteger.TEN,
            ledger.forSubnet(64)
                ?.paidTao
        )
    }

    @Test fun `identical identified rows are counted once but unidentified rows are preserved`() {
        val buy = operation(
            "10",
            "100",
            "0.1",
            "1"
        )
        assertEquals(
            precision * BigInteger.TEN,
            BittensorEstimatedCostBasis(listOf(buy, buy), true)
                .forSubnet(64)
                ?.paidTao
        )
        val unidentified = buy.copy(eventId = "")
        assertEquals(
            precision * BigInteger.valueOf(20),
            BittensorEstimatedCostBasis(listOf(unidentified, unidentified), true)
                .forSubnet(64)
                ?.paidTao
        )
    }

    @Test fun `complete sale removes old cost before another purchase`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "10",
                    "50",
                    "0.2",
                    "3"
                ),
                operation(
                    "100",
                    "10",
                    "0.1",
                    "2"
                ),
                operation(
                    "10",
                    "100",
                    "0.1",
                    "1"
                )
            ),
            true
        )
        assertEquals(
            0,
            BigDecimal("0.2").compareTo(
                requireNotNull(ledger.forSubnet(64)).averagePrice
            )
        )
    }

    @Test fun `negative provider quantities use magnitude like iOS`() {
        val totals = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "-10",
                    "100",
                    "-0.1",
                    "1"
                )
            ),
            true
        )
            .forSubnet(64)
        assertEquals(precision * BigInteger.TEN, totals?.paidTao)
    }

    @Test fun `root and zero movements do not pollute a subnet ledger`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "10",
                    "100",
                    "0.1",
                    "1"
                )
                    .copy(netuid = 0),
                operation(
                    "0",
                    "0",
                    "0",
                    "2"
                )
            ),
            true
        )
        assertTrue(ledger.canEstimate(64))
        assertNull(ledger.forSubnet(64))
    }

    @Test fun `invalid operation cannot be converted to no purchases`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation(
                    "unknown",
                    "100",
                    "0.1",
                    "1"
                )
            ),
            true
        )
        assertFalse(ledger.canEstimate(64))
    }

    @Test fun `estimated profit keeps a loss negative`() {
        val totals = BittensorEstimatedPurchaseTotals(precision * BigInteger.TEN, precision * BigInteger.valueOf(100))
        assertEquals(precision.negate(), totals.estimatedProfit(precision * BigInteger.valueOf(50), precision * BigInteger.valueOf(4)))
    }

    @Test fun `monthly projection compounds annual net rate and preserves a negative rate`() {
        val principal = BigDecimal(100)
        val month = bittensorMonthlyProjection(principal, BigDecimal(100))
        assertTrue(month > BigDecimal("5.94") && month < BigDecimal("5.95"))
        assertEquals(
            0,
            bittensorMonthlyProjection(principal, BigDecimal.ZERO)
                .signum()
        )
        assertTrue(
            bittensorMonthlyProjection(principal, BigDecimal(-10))
                .signum() < 0
        )
    }

    private fun operation(
        input: String,
        output: String,
        price: String,
        id: String
    ) = BittensorReportedOperation(
        64,
        "synthetic-hotkey",
        input,
        output,
        price,
        id,
        "2026-10-01T12:00:00Z",
        "synthetic-reference",
        null
    )
}
