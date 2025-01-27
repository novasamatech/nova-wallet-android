package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.UserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosUserStakeUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.RealMythosUserStakeUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.RealMythosStakeSummaryInteractor

@Module
interface MythosBindsModule {

    @Binds
    fun bindUserStakeRepository(implementation: RealUserStakeRepository): UserStakeRepository

    @Binds
    fun bindUserStakeUseCase(implementation: RealMythosUserStakeUseCase): MythosUserStakeUseCase

    @Binds
    fun bindStakeSummaryInteractor(implementation: RealMythosStakeSummaryInteractor): MythosStakeSummaryInteractor
}
