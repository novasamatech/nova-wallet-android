package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BittensorPickerNavigationTest {

    @Test
    fun `picker root row depends on the setup it replaces`() {
        assertEquals(BittensorPickerRootAction.OPEN, bittensorPickerRootAction(BittensorChooseSubnetPayload()))
        assertEquals(BittensorPickerRootAction.REPLACE_SUBNET_AMOUNT, bittensorPickerRootAction(BittensorChooseSubnetPayload(replaceAmount = true)))
        assertEquals(
            BittensorPickerRootAction.RETURN,
            bittensorPickerRootAction(BittensorChooseSubnetPayload(replaceAmount = true, replaceOrigin = BittensorReplaceOrigin.ROOT_STAKE))
        )
    }

    @Test
    fun `a forked flow keeps filters and browse state under a new id`() {
        val state = BittensorSelectionState(browseState = BittensorSubnetBrowseState(hideThinPools = true))

        val forked = state.forkedFlow()

        assertNotEquals(state.flowId, forked.flowId)
        assertEquals(state.copy(flowId = forked.flowId), forked)
    }

    @Test
    fun `an empty or zero amount asks for one`() {
        fun button(input: String, ready: Boolean = true, quoted: Boolean = true, exceeds: Boolean = false, loading: Boolean = false) =
            bittensorSubnetQuoteButtonState(loading, input, ready, exceeds, quoted, "Continue", "Enter amount…")

        assertEquals(DescriptiveButtonState.Loading, button("", loading = true))
        assertEquals(DescriptiveButtonState.Disabled("Enter amount…"), button(""))
        assertEquals(DescriptiveButtonState.Disabled("Enter amount…"), button("0"))
        assertEquals(DescriptiveButtonState.Disabled("Enter amount…"), button("0", ready = false))
        assertEquals(DescriptiveButtonState.Disabled("Continue"), button("1", ready = false))
        assertEquals(DescriptiveButtonState.Disabled("Continue"), button("1", exceeds = true))
        assertEquals(DescriptiveButtonState.Loading, button("1", quoted = false))
        assertEquals(DescriptiveButtonState.Enabled("Continue"), button("1"))
    }
}
