package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.exit

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedPurchaseTotals
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorPositionCostBasis
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorSaleCostBasisTest {

    private val purchase = BittensorReportedOperation(1, "hotkey", "10", "100", "0.1", "buy", "2026-10-01T12:00:00Z", "purchase", "buy")

    private val withPurchase = BittensorEstimatedCostBasis(listOf(purchase), true)

    private val noPurchases = BittensorEstimatedCostBasis(emptyList(), true)

    private val purchaseTotals = requireNotNull(withPurchase.forSubnet(1))

    private val carried = BittensorEstimatedPurchaseTotals(BigInteger.valueOf(7), BigInteger.valueOf(70))

    private fun loaded(basis: BittensorEstimatedCostBasis?, settling: Boolean = false) =
        BittensorPositionCostBasis(ExtendedLoadingState.Loaded(basis), settling)

    @Test
    fun `settled history distinguishes purchases from no purchases`() {
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Known(purchaseTotals, true)), resolveSaleCostBasis(loaded(withPurchase), 1))
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.NoPurchases), resolveSaleCostBasis(loaded(noPurchases), 1))
    }

    @Test
    fun `settling history is never reported as no purchases`() {
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Unknown), resolveSaleCostBasis(loaded(noPurchases, settling = true), 1))
        assertEquals(
            ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Known(purchaseTotals, false)),
            resolveSaleCostBasis(loaded(withPurchase, settling = true), 1)
        )
    }

    @Test
    fun `incomplete or missing history stays unknown`() {
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Unknown), resolveSaleCostBasis(loaded(null), 1))
        val incomplete = BittensorEstimatedCostBasis(listOf(purchase), false)
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Unknown), resolveSaleCostBasis(loaded(incomplete), 1))
        val failed = BittensorPositionCostBasis(ExtendedLoadingState.Error(IllegalStateException()), false)
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Unknown), resolveSaleCostBasis(failed, 1))
    }

    @Test
    fun `review keeps the setup totals while its own history reloads or fails`() {
        val known = ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Known(carried, true))
        assertEquals(known, resolveSaleCostBasis(BittensorPositionCostBasis(ExtendedLoadingState.Loading, false), 1, carried))
        assertEquals(known, resolveSaleCostBasis(BittensorPositionCostBasis(ExtendedLoadingState.Error(IllegalStateException()), false), 1, carried))
        assertEquals(known, resolveSaleCostBasis(loaded(null), 1, carried))
        assertEquals(known, resolveSaleCostBasis(loaded(withPurchase, settling = true), 1, carried))
        assertEquals(ExtendedLoadingState.Loaded(BittensorSaleCostBasis.Known(purchaseTotals, true)), resolveSaleCostBasis(loaded(withPurchase), 1, carried))
    }

    @Test
    fun `only settled totals are carried to the review`() {
        assertEquals(purchaseTotals, BittensorSaleCostBasis.Known(purchaseTotals, true).carriedPayload()?.totals())
        assertNull(BittensorSaleCostBasis.Known(purchaseTotals, false).carriedPayload())
        assertNull(BittensorSaleCostBasis.NoPurchases.carriedPayload())
    }
}
