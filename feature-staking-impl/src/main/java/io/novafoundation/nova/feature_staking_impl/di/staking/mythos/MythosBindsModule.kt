package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosCandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosCandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.UserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.RealMythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.RealMythosCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.RealMythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.RealStartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.StartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.RealMythosCollatorFormatter

@Module
interface MythosBindsModule {

    @Binds
    fun bindUserStakeRepository(implementation: RealUserStakeRepository): UserStakeRepository

    @Binds
    fun bindStakingRepository(implementation: RealMythosStakingRepository): MythosStakingRepository

    @Binds
    fun bindSessionRepository(implementation: RealMythosSessionRepository): MythosSessionRepository

    @Binds
    fun bindMythosCandidateRepository(implementation: RealMythosCandidatesRepository): MythosCandidatesRepository

    @Binds
    fun bindCollatorProvider(implementation: RealMythosCollatorProvider): MythosCollatorProvider

    @Binds
    fun bindUserStakeUseCase(implementation: RealMythosDelegatorStateUseCase): MythosDelegatorStateUseCase

    @Binds
    fun bindStakeSummaryInteractor(implementation: RealMythosStakeSummaryInteractor): MythosStakeSummaryInteractor

    @Binds
    fun bindStartStakingInteractor(implementation: RealStartMythosStakingInteractor): StartMythosStakingInteractor

    @Binds
    fun bindMythosCollatorFormatter(implementation: RealMythosCollatorFormatter): MythosCollatorFormatter
}
