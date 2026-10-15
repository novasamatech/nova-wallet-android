package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository

class BittensorPortfolioInteractor(
    private val repository: BittensorPortfolioRepository,
    private val subnetRepository: BittensorSubnetRepository
) {

    suspend fun subnets() = subnetRepository.subnets()

    fun observeAsset() = repository.observeAsset()

    fun observeRootPortfolio() = repository.observeRootPortfolio()

    fun observePortfolio() = repository.observePortfolio()
}
