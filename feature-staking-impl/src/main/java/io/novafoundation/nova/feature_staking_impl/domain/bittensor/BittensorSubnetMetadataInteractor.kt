package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository

class BittensorSubnetMetadataInteractor(private val subnetRepository: BittensorSubnetRepository) {

    suspend fun subnet(netuid: BittensorNetuid.Subnet): BittensorSubnet {
        val subnets = subnetRepository.subnets()
        return subnets.first { it.id == netuid }
    }
}
