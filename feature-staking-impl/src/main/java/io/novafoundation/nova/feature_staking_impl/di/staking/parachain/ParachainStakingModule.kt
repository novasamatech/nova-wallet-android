package io.novafoundation.nova.feature_staking_impl.di.staking.parachain

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
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
import io.novafoundation.nova.feature_staking_impl.data.parachainStaking.turing.repository.TuringStakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingPeriodRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.start.StartParachainStakingFlowModule
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.turing.TuringStakingModule
import io.novafoundation.nova.feature_staking_impl.di.staking.parachain.unbond.ParachainStakingUnbondModule
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.CollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.RealCollatorProvider
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.RealCollatorsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.recommendations.CollatorRecommendatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.ParachainNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.ParachainStakingAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.RealParachainStakingAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.stakeSummary.ParachainStakingStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.unbondings.ParachainStakingUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.userRewards.ParachainStakingUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.rewards.ParachainStakingRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.ParachainStakingHintsUseCase
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import io.novafoundation.nova.runtime.repository.TotalIssuanceRepository
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(includes = [StartParachainStakingFlowModule::class, ParachainStakingUnbondModule::class, TuringStakingModule::class])
class ParachainStakingModule {

    @Provides
    @FeatureScope
    fun provideDelegatorStateRepository(
        @Named(LOCAL_STORAGE_SOURCE) localDataSource: StorageDataSource,
        @Named(REMOTE_STORAGE_SOURCE) remoteDataSource: StorageDataSource,
    ): DelegatorStateRepository = RealDelegatorStateRepository(localStorage = localDataSource, remoteStorage = remoteDataSource)

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
        currentRoundRepository: CurrentRoundRepository
    ): RoundDurationEstimator = RealRoundDurationEstimator(parachainStakingConstantsRepository, chainStateRepository, currentRoundRepository)

    @Provides
    @FeatureScope
    fun provideRewardCalculatorFactory(
        rewardsRepository: RewardsRepository,
        commonStakingRepository: TotalIssuanceRepository,
        currentRoundRepository: CurrentRoundRepository,
        turingStakingRewardsRepository: TuringStakingRewardsRepository,
    ) = ParachainStakingRewardCalculatorFactory(rewardsRepository, currentRoundRepository, commonStakingRepository, turingStakingRewardsRepository)

    @Provides
    @FeatureScope
    fun provideDelegatorStateUseCase(
        repository: DelegatorStateRepository,
        stakingSharedState: StakingSharedState,
        accountRepository: AccountRepository
    ) = DelegatorStateUseCase(repository, stakingSharedState, accountRepository)

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
        identityRepository: OnChainIdentityRepository,
        parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
        candidatesRepository: CandidatesRepository,
        rewardCalculatorFactory: ParachainStakingRewardCalculatorFactory,
        chainRegistry: ChainRegistry,
    ): CollatorProvider = RealCollatorProvider(
        identityRepository = identityRepository,
        currentRoundRepository = currentRoundRepository,
        parachainStakingConstantsRepository = parachainStakingConstantsRepository,
        rewardCalculatorFactory = rewardCalculatorFactory,
        chainRegistry = chainRegistry,
        candidatesRepository = candidatesRepository
    )

    @Provides
    @FeatureScope
    fun provideParachainStakingHintsUseCase(
        stakingSharedState: StakingSharedState,
        resourceManager: ResourceManager,
        roundDurationEstimator: RoundDurationEstimator
    ) = ParachainStakingHintsUseCase(stakingSharedState, resourceManager, roundDurationEstimator)

    @Provides
    @FeatureScope
    fun provideCollatorRecommendatorFactory(
        collatorProvider: CollatorProvider,
        computationalCache: ComputationalCache
    ) = CollatorRecommendatorFactory(collatorProvider, computationalCache)

    @Provides
    @FeatureScope
    fun provideTotalRewardsInteractor(
        stakingRewardsRepository: StakingRewardsRepository,
        stakingPeriodRepository: StakingPeriodRepository
    ) = ParachainStakingUserRewardsInteractor(stakingRewardsRepository, stakingPeriodRepository)

    @Provides
    @FeatureScope
    fun provideCollatorConstantsUseCase(
        parachainStakingConstantsRepository: ParachainStakingConstantsRepository,
        stakingSharedState: StakingSharedState,
        collatorProvider: CollatorProvider,
        addressIconGenerator: AddressIconGenerator
    ): CollatorsUseCase = RealCollatorsUseCase(stakingSharedState, parachainStakingConstantsRepository, collatorProvider, addressIconGenerator)

    @Provides
    @FeatureScope
    fun provideStakeSummaryInteractor(
        currentRoundRepository: CurrentRoundRepository,
        roundDurationEstimator: RoundDurationEstimator,
        candidatesRepository: CandidatesRepository
    ) = ParachainStakingStakeSummaryInteractor(currentRoundRepository, candidatesRepository, roundDurationEstimator)

    @Provides
    @FeatureScope
    fun provideUnbondingInteractor(
        delegatorStateRepository: DelegatorStateRepository,
        currentRoundRepository: CurrentRoundRepository,
        roundDurationEstimator: RoundDurationEstimator,
        identityRepository: OnChainIdentityRepository
    ) = ParachainStakingUnbondingsInteractor(delegatorStateRepository, currentRoundRepository, roundDurationEstimator, identityRepository)

    @Provides
    @FeatureScope
    fun provideAlertsInteractor(
        candidatesRepository: CandidatesRepository,
        currentRoundRepository: CurrentRoundRepository,
        delegatorStateRepository: DelegatorStateRepository,
    ): ParachainStakingAlertsInteractor {
        return RealParachainStakingAlertsInteractor(candidatesRepository, currentRoundRepository, delegatorStateRepository)
    }
}
