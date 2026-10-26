package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSubnetTradingAvailabilityRepository
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorSwapSource
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickInteractor
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickSelector
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRecommendedYieldInteractor
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named
import kotlin.random.Random

@Module
class BittensorQuickPickModule {

    @Provides
    fun selector(): BittensorQuickPickSelector {
        val random = Random.Default
        return BittensorQuickPickSelector(random)
    }

    @Provides
    fun availability(@Named(REMOTE_STORAGE_SOURCE) storage: StorageDataSource) = BittensorSubnetTradingAvailabilityRepository(storage)

    @Provides
    fun interactor(
        repository: BittensorSubnetRepository,
        availability: BittensorSubnetTradingAvailabilityRepository,
        swaps: BittensorSwapSource,
        selector: BittensorQuickPickSelector,
        yields: BittensorRecommendedYieldInteractor
    ) = BittensorQuickPickInteractor(repository, availability, swaps, selector, yields)
}
