package io.novafoundation.nova.feature_staking_impl.di.staking.mythos

import dagger.Binds
import dagger.Module
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosCandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.MythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosCandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosSessionRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.mythos.repository.RealMythosUserStakeRepository
import io.novafoundation.nova.feature_staking_impl.domain.common.RealStakingBlockNumberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingBlockNumberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.RealMythosDelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.MythosCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.collator.RealMythosCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.MythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.stakeSummary.RealMythosStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.RealStartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.start.StartMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.RealUnbondMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.UnbondMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.MythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.RealMythosCollatorFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.RealMythosStakingValidationFailureFormatter

@Module
interface MythosBindsModule {

    @Binds
    fun bindUserStakeRepository(implementation: RealMythosUserStakeRepository): MythosUserStakeRepository

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
    fun bindUnbondInteractor(implementation: RealUnbondMythosStakingInteractor): UnbondMythosStakingInteractor

    @Binds
    fun bindMythosCollatorFormatter(implementation: RealMythosCollatorFormatter): MythosCollatorFormatter

    @Binds
    fun bindBlockNumberUseCase(implementation: RealStakingBlockNumberUseCase): StakingBlockNumberUseCase

    @Binds
    fun bindValidationFormatter(implementation: RealMythosStakingValidationFailureFormatter): MythosStakingValidationFailureFormatter
}
