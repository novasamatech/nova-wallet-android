package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.parachain.ParachainNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.parachain.ParachainStakeSummaryComponentFactory

@Module
class ParachainModule {

    @Provides
    @ScreenScope
    fun provideParachainStakeSummaryComponentFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        resourceManager: ResourceManager,
    ) = ParachainStakeSummaryComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
    )

    @Provides
    @ScreenScope
    fun provideParachainNetworkInfoComponentFactory(
        delegatorStateUseCase: DelegatorStateUseCase,
        parachainNetworkInfoInteractor: ParachainNetworkInfoInteractor,
        resourceManager: ResourceManager,
    ) = ParachainNetworkInfoComponentFactory(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        interactor = parachainNetworkInfoInteractor,
    )
}
