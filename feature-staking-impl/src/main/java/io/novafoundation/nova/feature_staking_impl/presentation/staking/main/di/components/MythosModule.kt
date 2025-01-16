package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.components

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary.mythos.MythosStakeSummaryComponentFactory

@Module
class MythosModule {

    @Provides
    @ScreenScope
    fun provideStakeSummaryComponentFactory(
        mythosSharedComputation: MythosSharedComputation,
        interactor: MythosStakeSummaryInteractor,
    ) = MythosStakeSummaryComponentFactory(
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor
    )
}
