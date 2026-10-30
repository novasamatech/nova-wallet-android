package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorReportedOperation
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorPositionCostBasis
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorBuyMoreCostBasisTest {

    private val purchase = BittensorReportedOperation(1, "hotkey", "10", "100", "0.1", "StakeAdded", "2026-10-01T12:00:00Z", "purchase", "buy")

    private val withPurchase = BittensorEstimatedCostBasis(listOf(purchase), true)

    private val noPurchases = BittensorEstimatedCostBasis(emptyList(), true)

    private fun loaded(basis: BittensorEstimatedCostBasis?, settling: Boolean) =
        BittensorPositionCostBasis(ExtendedLoadingState.Loaded(basis), settling)

    @Test
    fun `settled history without purchases still reads as no purchases`() {
        assertEquals(ExtendedLoadingState.Loaded(noPurchases), loaded(noPurchases, settling = false).buyMoreCostBasis(1))
    }

    @Test
    fun `settling history without the own purchase is unknown instead of no purchases`() {
        assertEquals(ExtendedLoadingState.Loaded(null), loaded(noPurchases, settling = true).buyMoreCostBasis(1))
    }

    @Test
    fun `settling history keeps the purchases it already has`() {
        assertEquals(ExtendedLoadingState.Loaded(withPurchase), loaded(withPurchase, settling = true).buyMoreCostBasis(1))
    }
}
