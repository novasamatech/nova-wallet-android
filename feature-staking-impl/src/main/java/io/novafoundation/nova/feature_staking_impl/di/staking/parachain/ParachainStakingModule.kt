package io.novafoundation.nova.feature_staking_impl.di.staking.parachain

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_api.domain.api.IdentityRepository
import io.novafoundation.nova.feature_staking_impl.data.common.repository.CommonStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RealRoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.RoundDurationEstimator
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.CurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.DelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.ParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RealCandidatesRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RealCurrentRoundRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RealDelegatorStateRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RealRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.repository.RuntimeParachainStakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.start.StartParachainStakingFlowModule
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.RealCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(includes = [StartParachainStakingFlowModule::class])
class ParachainStakingModule {

    @Provides
    @FeatureScope
    fun provideDelegatorStateRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): DelegatorStateRepository = RealDelegatorStateRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideCurrentRoundRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): CurrentRoundRepository = RealCurrentRoundRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideRewardsRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): RewardsRepository = RealRewardsRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideCandidatesRepository(
        @Named(REMOTE_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): CandidatesRepository = RealCandidatesRepository(storageDataSource)

    @Provides
    @FeatureScope
    fun provideConstantsRepository(
        chainRegistry: ChainRegistry
    ): ParachainStakingConstantsRepository = RuntimeParachainStakingConstantsRepository(chainRegistry)

    @Provides
    @FeatureScope
    fun provideRoundDurationEstimator(
        parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
        chainStateRepository: ChainStateRepository,
    ): RoundDurationEstimator = RealRoundDurationEstimator(parachainStakingConstantsRepository, chainStateRepository)

    @Provides
    @FeatureScope
    fun provideRewardCalculatorFactory(
        rewardsRepository: RewardsRepository,
        commonStakingRepository: CommonStakingRepository,
        currentRoundRepository: CurrentRoundRepository,
    ) = ParachainStakingRewardCalculatorFactory(rewardsRepository, currentRoundRepository, commonStakingRepository)

    @Provides
    @FeatureScope
    fun provideDelegatorStateUseCase(
        repository: DelegatorStateRepository,
        accountRepository: AccountRepository
    ) = DelegatorStateUseCase(repository, accountRepository)

    @Provides
    @FeatureScope
    fun provideNetworkInfoInteractor(
        currentRoundRepository: CurrentRoundRepository,
        parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
        roundDurationEstimator: RoundDurationEstimator
    ) = ParachainNetworkInfoInteractor(currentRoundRepository, parachainStakingConstantsRepository, roundDurationEstimator)

    @Provides
    @FeatureScope
    fun provideCollatorProvider(
        currentRoundRepository: CurrentRoundRepository,
        identityRepository: IdentityRepository,
        parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
    ): CollatorProvider = RealCollatorProvider(identityRepository, currentRoundRepository, parachainStakingConstantsRepository)
}
