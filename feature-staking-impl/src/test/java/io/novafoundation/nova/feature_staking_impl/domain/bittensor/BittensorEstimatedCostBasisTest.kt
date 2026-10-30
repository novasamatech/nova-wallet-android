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

    @Test
    fun `price tolerance includes both boundaries`() {
        val amounts = listOf("2", "4.5")
        amounts.forEach { amount ->
            val purchase = operation(amount, "30", "0.1", "1")
            val ledger = BittensorEstimatedCostBasis(listOf(purchase), true)

            assertTrue(ledger.canEstimate(64))
            assertEquals(BigDecimal(amount).movePointRight(9).toBigInteger(), ledger.forSubnet(64)?.paidTao)
        }
    }

    @Test
    fun `price outside tolerance is unclassified`() {
        val amounts = listOf("1.999999999", "4.500000001")
        amounts.forEach { amount ->
            val purchase = operation(amount, "30", "0.1", "1")
            val ledger = BittensorEstimatedCostBasis(listOf(purchase), true)

            assertFalse(ledger.canEstimate(64))
        }
    }

    @Test
    fun `non trade labels support separators camel case and acronyms`() {
        val labels = listOf(
            "move", "TRANSFER", "stake_transfer", "moveStake", "stakeTransfer", "TAOTransfer", "stake-move",
            "moved", "stakeMoved", "transferred", "STAKETransferred", " stake--transferred "
        )
        labels.forEach { label ->
            val movement = operation("10", "100", "0.1", "1").copy(type = label)
            val ledger = BittensorEstimatedCostBasis(listOf(movement), true)

            assertTrue(label, ledger.canEstimate(64))
            assertNull(label, ledger.forSubnet(64))
        }
    }

    @Test
    fun `non trade matching requires a whole word`() {
        val labels = listOf(null, "", "removeStake", "transferable", "movement")
        labels.forEach { label ->
            val purchase = operation("10", "100", "0.1", "1").copy(type = label)
            val ledger = BittensorEstimatedCostBasis(listOf(purchase), true)

            assertEquals(precision * BigInteger.TEN, ledger.forSubnet(64)?.paidTao)
        }
    }

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
                    "5",
                    "50",
                    "0.1",
                    "2",
                    event = "StakeRemoved"
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

    @Test
    fun `event name sets the trade direction`() {
        val ledger = BittensorEstimatedCostBasis(
            listOf(
                operation("4.6", "4", "1.2", "2", event = "StakeRemoved"),
                operation("10", "8", "1.2", "1")
            ),
            true
        )

        assertEquals(precision * BigInteger.valueOf(5), ledger.forSubnet(64)?.paidTao)
        assertEquals(precision * BigInteger.valueOf(4), ledger.forSubnet(64)?.receivedAlpha)
    }

    @Test
    fun `unknown events are skipped even when malformed`() {
        for (event in listOf("StakeMoved", "stakeAdded", "STAKE_REMOVED", "buy", "")) {
            val ledger = BittensorEstimatedCostBasis(
                listOf(operation("invalid", "invalid", "invalid", "2", event = event), operation("10", "100", "0.1", "1")),
                true
            )

            assertTrue(event, ledger.canEstimate(64))
            assertEquals(event, precision * BigInteger.TEN, ledger.forSubnet(64)?.paidTao)
        }
    }

    @Test
    fun `sale outside price tolerance is unclassified`() {
        val sale = operation("1", "1", "100", "2", event = "StakeRemoved")
        val ledger = BittensorEstimatedCostBasis(listOf(sale, operation("10", "100", "0.1", "1")), true)

        assertFalse(ledger.canEstimate(64))
    }

    @Test
    fun `non trade label takes priority over malformed amounts`() {
        val movement = operation("invalid", "invalid", "invalid", "1").copy(type = "stakeTransferredBuy")
        val ledger = BittensorEstimatedCostBasis(listOf(movement), true)

        assertTrue(ledger.canEstimate(64))
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
            listOf(buy.copy(reference = "3", type = "transferStake"), buy.copy(reference = "2", type = "move_stake"), buy),
            true
        )
        assertEquals(
            precision * BigInteger.TEN,
            ledger.forSubnet(64)
                ?.paidTao
        )
    }

    @Test fun `identical rows are counted once`() {
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
        assertEquals(
            precision * BigInteger.valueOf(20),
            BittensorEstimatedCostBasis(listOf(buy, buy.copy(reference = "2")), true)
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
                    "10",
                    "100",
                    "0.1",
                    "2",
                    event = "StakeRemoved"
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
        reference: String,
        event: String = "StakeAdded"
    ) = BittensorReportedOperation(
        64,
        "synthetic-hotkey",
        input,
        output,
        price,
        event,
        "2026-10-01T12:00:00Z",
        reference,
        "trade"
    )
}
