package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import kotlinx.coroutines.flow.map

class BittensorSubnetMetadataInteractor(private val subnetRepository: BittensorSubnetRepository) {

    fun observeSubnet(netuid: BittensorNetuid.Subnet) = subnetRepository.observeSubnets().map { subnets -> subnets.first { it.id == netuid } }

    suspend fun subnet(netuid: BittensorNetuid.Subnet): BittensorSubnet {
        val subnets = subnetRepository.subnets()
        return subnets.first { it.id == netuid }
    }
}
