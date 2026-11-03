package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorEstimatedCostBasis
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common.BittensorPositionCostBasis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPositionEarnedUiTest {

    @Test
    fun `settling loading applies visibility after progress makes cell visible`() {
        val basis = BittensorPositionCostBasis(ExtendedLoadingState.Loading, isProvisional = true)
        val subnet = BittensorNetuid.Subnet(1)
        val model = BittensorPositionEarnedUi(ExtendedLoadingState.Loading, basis.showsEarned(subnet))
        var visible = false
        val calls = mutableListOf<String>()

        model.bind(
            renderValue = {
                assertEquals(ExtendedLoadingState.Loading, it)
                visible = true // TableCellView.showProgress() behavior
                calls += "progress"
            },
            renderVisibility = {
                visible = it
                calls += "visibility"
            }
        )

        assertEquals(listOf("progress", "visibility"), calls)
        assertFalse(visible)
    }

    @Test
    fun `no purchases hides earned but incomplete unavailable and failed estimates do not claim no purchases`() {
        val subnet = BittensorNetuid.Subnet(1)
        val empty = BittensorEstimatedCostBasis(emptyList(), true)
        val incomplete = BittensorEstimatedCostBasis(emptyList(), false)
        val noPurchases = BittensorPositionCostBasis(ExtendedLoadingState.Loaded(empty), isProvisional = false)
        val partial = BittensorPositionCostBasis(ExtendedLoadingState.Loaded(incomplete), isProvisional = false)
        val unavailable = BittensorPositionCostBasis(ExtendedLoadingState.Loaded(null), isProvisional = false)
        val failure = IllegalStateException("offline")
        val failed = BittensorPositionCostBasis(ExtendedLoadingState.Error(failure), isProvisional = false)

        assertFalse(noPurchases.showsEarned(subnet))
        assertTrue(partial.showsEarned(subnet))
        assertTrue(unavailable.showsEarned(subnet))
        assertTrue(failed.showsEarned(subnet))
        assertFalse(failed.copy(isProvisional = true).showsEarned(subnet))
    }
}
