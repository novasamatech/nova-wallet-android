package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorPortfolioRepository

class BittensorPortfolioInteractor(private val repository: BittensorPortfolioRepository) {

    fun observeAsset() = repository.observeAsset()

    fun observePortfolio() = repository.observePortfolio()
}
