package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosStakingRepository
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
    fun bindStakingRepository(implementation: RealMythosStakingRepository): MythosStakingRepository

    @Binds
    fun bindSessionRepository(implementation: RealMythosSessionRepository): MythosSessionRepository

    @Binds
    fun bindUserStakeUseCase(implementation: RealMythosUserStakeUseCase): MythosUserStakeUseCase

    @Binds
    fun bindStakeSummaryInteractor(implementation: RealMythosStakeSummaryInteractor): MythosStakeSummaryInteractor
}
