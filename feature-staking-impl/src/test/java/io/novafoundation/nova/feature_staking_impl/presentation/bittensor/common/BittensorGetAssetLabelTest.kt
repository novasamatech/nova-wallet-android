package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorGetAssetLabelTest {

    @Test
    fun `visible states take the bittensor label`() {
        assertEquals(DescriptiveButtonState.Enabled("Swap for TAO"), DescriptiveButtonState.Enabled("Get TAO").withBittensorGetAssetLabel("Swap for TAO"))
        assertEquals(DescriptiveButtonState.Disabled("Swap for TAO"), DescriptiveButtonState.Disabled("Get TAO").withBittensorGetAssetLabel("Swap for TAO"))
    }

    @Test
    fun `hidden states and a missing label are kept`() {
        assertEquals(DescriptiveButtonState.Gone, DescriptiveButtonState.Gone.withBittensorGetAssetLabel("Swap for TAO"))
        assertEquals(DescriptiveButtonState.Enabled("Get TAO"), DescriptiveButtonState.Enabled("Get TAO").withBittensorGetAssetLabel(null))
    }
}
