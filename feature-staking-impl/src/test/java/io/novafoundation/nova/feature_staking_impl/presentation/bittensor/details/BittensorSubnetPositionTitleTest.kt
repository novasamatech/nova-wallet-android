package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSubnetPositionTitleTest {

    @Test
    fun `catalogue name and alpha symbol replace numbered placeholder`() {
        val subnet = BittensorSubnet(BittensorSubnetId(1), "Crucible", "α", null)
        assertEquals("Crucible α", subnetPositionTitle(subnet, "Subnet 1"))
    }

    @Test
    fun `unavailable catalogue retains a numbered position`() {
        assertEquals("Subnet 1", subnetPositionTitle(null, "Subnet 1"))
    }

    @Test
    fun `blank metadata does not produce an empty title`() {
        val subnet = BittensorSubnet(BittensorSubnetId(1), "", "", null)
        assertEquals("Subnet 1", subnetPositionTitle(subnet, "Subnet 1"))
    }
}
