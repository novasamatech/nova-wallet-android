package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorConfigRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BittensorSubnetMetadataInteractor(
    private val subnets: BittensorSubnetRepository,
    private val config: BittensorConfigRepository
) {

    suspend fun subnet(netuid: BittensorNetuid.Subnet): BittensorSubnet = coroutineScope {
        val configuration = async { config.config() }
        val subnet = subnets.subnets().first { it.id.value == netuid.value }
        val configuredLogo = configuration.await().subnetLogos[netuid]
        subnet.copy(logoUrl = configuredLogo ?: subnet.logoUrl)
    }
}
