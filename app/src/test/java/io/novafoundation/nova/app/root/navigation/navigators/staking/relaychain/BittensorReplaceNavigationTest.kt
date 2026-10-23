package io.novafoundation.nova.app.root.navigation.navigators.staking.relaychain

import io.novafoundation.nova.app.R
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorReplaceOrigin
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.BittensorSubnetAmountPayload
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorReplaceNavigationTest {

    @Test
    fun `a plain pick opens the subnet setup on top`() {
        assertEquals(R.id.action_open_bittensor_subnet_amount, bittensorSubnetAmountAction(BittensorSubnetAmountPayload(subnetId = 1)))
    }

    @Test
    fun `a pick from the subnet setup picker replaces that setup`() {
        assertEquals(R.id.action_replace_bittensor_subnet_amount, bittensorSubnetAmountAction(BittensorSubnetAmountPayload(subnetId = 1, replaceAmount = true)))
    }

    @Test
    fun `a pick from the root setup picker replaces the root setup`() {
        val payload = BittensorSubnetAmountPayload(subnetId = 1, replaceAmount = true, replaceOrigin = BittensorReplaceOrigin.ROOT_STAKE)

        assertEquals(R.id.action_replace_bittensor_root_staking_with_subnet_amount, bittensorSubnetAmountAction(payload))
    }
}
