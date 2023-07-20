package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo.NominationPoolsNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools.NominationPoolsNetworkInfoComponentFactory

@Module
class NominationPoolsModule {

    @Provides
    @ScreenScope
    fun provideParachainNetworkInfoComponentFactory(
        parachainNetworkInfoInteractor: NominationPoolsNetworkInfoInteractor,
        resourceManager: ResourceManager,
    ) = NominationPoolsNetworkInfoComponentFactory(
        resourceManager = resourceManager,
        interactor = parachainNetworkInfoInteractor,
    )
}
