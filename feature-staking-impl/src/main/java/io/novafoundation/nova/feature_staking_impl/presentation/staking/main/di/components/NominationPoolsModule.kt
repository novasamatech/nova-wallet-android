package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo.NominationPoolsNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary.NominationPoolStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings.NominationPoolUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo.nominationPools.NominationPoolsNetworkInfoComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.nominationPools.NominationPoolsStakeSummaryComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.nominationPools.NominationPoolsUnbondingComponentFactory

@Module
class NominationPoolsModule {

    @Provides
    @ScreenScope
    fun provideNetworkInfoComponentFactory(
        parachainNetworkInfoInteractor: NominationPoolsNetworkInfoInteractor,
        resourceManager: ResourceManager,
    ) = NominationPoolsNetworkInfoComponentFactory(
        resourceManager = resourceManager,
        interactor = parachainNetworkInfoInteractor,
    )

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        interactor: NominationPoolStakeSummaryInteractor,
        pollMemberUseCase: NominationPoolMemberUseCase
    ) = NominationPoolsStakeSummaryComponentFactory(
        poolMemberUseCase = pollMemberUseCase,
        interactor = interactor
    )

    @Provides
    @ScreenScope
    fun provideUnbondComponentFactory(
        interactor: NominationPoolUnbondingsInteractor,
        pollMemberUseCase: NominationPoolMemberUseCase
    ) = NominationPoolsUnbondingComponentFactory(
        poolMemberUseCase = pollMemberUseCase,
        interactor = interactor
    )
}
