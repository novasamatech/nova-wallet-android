package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class BittensorSubnetPresentationTest {

    private val resources = mock(ResourceManager::class.java).also {
        `when`(it.getString(R.string.bittensor_subnet_number, 42)).thenReturn("Subnet 42")
    }

    @Test
    fun `unnamed subnet reads Subnet netuid and is never blank in copy`() {
        val subnet = BittensorSubnet(BittensorNetuid.Subnet(42), "", "SN42", null)

        assertEquals("Subnet 42", subnet.title(resources))
        assertEquals("Subnet 42 SN42", subnet.displayName(resources))
    }

    @Test
    fun `named subnet keeps its name`() {
        val subnet = BittensorSubnet(BittensorNetuid.Subnet(42), "Chutes", "ش", null)

        assertEquals("Chutes", subnet.title(resources))
        assertEquals("Chutes ش", subnet.displayName(resources))
    }
}
