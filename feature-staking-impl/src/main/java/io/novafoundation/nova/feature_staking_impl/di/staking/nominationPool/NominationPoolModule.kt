package io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.datasource.KnownMaxUnlockingOverwrites
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.datasource.RealKnownMaxUnlockingOverwrites
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolImageDataSource
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PredefinedPoolImageDataSource
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.RealPoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.RealNominationPoolGlobalsRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.RealNominationPoolMembersRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.RealNominationPoolStateRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.RealNominationPoolUnbondRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingConstantsRepository
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.RealNominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.NominationPoolHintsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.hints.RealNominationPoolHintsUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculatorFactory
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.NominationPoolsAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.RealNominationPoolsAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo.NominationPoolsNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.networkInfo.RealNominationPoolsNetworkInfoInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary.NominationPoolStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.stakeSummary.RealNominationPoolStakeSummaryInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings.NominationPoolUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.unbondings.RealNominationPoolUnbondingsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.userRewards.NominationPoolsUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.userRewards.RealNominationPoolsUserRewardsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool.NominationPoolYourPoolInteractor
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.yourPool.RealNominationPoolYourPoolInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.PoolDisplayFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common.RealPoolDisplayFormatter
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module(includes = [NominationPoolsValidationsModule::class])
class NominationPoolModule {

    @Provides
    @FeatureScope
    fun providePoolAccountDerivation(
        @Named(LOCAL_STORAGE_SOURCE) dataSource: StorageDataSource
    ): PoolAccountDerivation = RealPoolAccountDerivation(dataSource)

    @Provides
    @FeatureScope
    fun providePoolImageDataSource(): PoolImageDataSource = PredefinedPoolImageDataSource()

    @Provides
    @FeatureScope
    fun providePoolDisplayFormatter(
        addressIconGenerator: AddressIconGenerator
    ): PoolDisplayFormatter = RealPoolDisplayFormatter(addressIconGenerator)

    @Provides
    @FeatureScope
    fun provideNominationPoolBalanceRepository(
        @Named(LOCAL_STORAGE_SOURCE) localDataSource: StorageDataSource,
        @Named(REMOTE_STORAGE_SOURCE) remoteDataSource: StorageDataSource,
        poolImageDataSource: PoolImageDataSource,
    ): NominationPoolStateRepository = RealNominationPoolStateRepository(
        localStorage = localDataSource,
        remoteStorage = remoteDataSource,
        poolImageDataSource = poolImageDataSource,
    )

    @Provides
    @FeatureScope
    fun provideKnownMaxUnlockingOverwrites(): KnownMaxUnlockingOverwrites = RealKnownMaxUnlockingOverwrites()

    @Provides
    @FeatureScope
    fun provideNominationPoolGlobalsRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
        knownMaxUnlockingOverwrites: KnownMaxUnlockingOverwrites,
        stakingRepository: StakingConstantsRepository,
    ): NominationPoolGlobalsRepository {
        return RealNominationPoolGlobalsRepository(localStorageSource, knownMaxUnlockingOverwrites, stakingRepository)
    }

    @Provides
    @FeatureScope
    fun provideNominationPoolMembersRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource,
        multiChainRuntimeCallsApi: MultiChainRuntimeCallsApi,
    ): NominationPoolMembersRepository {
        return RealNominationPoolMembersRepository(localStorageSource, multiChainRuntimeCallsApi)
    }

    @Provides
    @FeatureScope
    fun provideNominationPoolUnbondRepository(
        @Named(LOCAL_STORAGE_SOURCE) dataSource: StorageDataSource
    ): NominationPoolUnbondRepository = RealNominationPoolUnbondRepository(dataSource)

    @Provides
    @FeatureScope
    fun provideNominationPoolMembersUseCase(
        accountRepository: AccountRepository,
        nominationPoolMembersRepository: NominationPoolMembersRepository,
        stakingSharedState: StakingSharedState,
    ): NominationPoolMemberUseCase {
        return RealNominationPoolMemberUseCase(
            accountRepository = accountRepository,
            stakingSharedState = stakingSharedState,
            nominationPoolMembersRepository = nominationPoolMembersRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideNetworkInfoInteractor(
        relaychainStakingSharedComputation: StakingSharedComputation,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
        poolAccountDerivation: PoolAccountDerivation,
        relaychainStakingInteractor: StakingInteractor,
        nominationPoolMemberUseCase: NominationPoolMemberUseCase,
    ): NominationPoolsNetworkInfoInteractor = RealNominationPoolsNetworkInfoInteractor(
        relaychainStakingSharedComputation = relaychainStakingSharedComputation,
        nominationPoolGlobalsRepository = nominationPoolGlobalsRepository,
        poolAccountDerivation = poolAccountDerivation,
        relaychainStakingInteractor = relaychainStakingInteractor,
        nominationPoolMemberUseCase = nominationPoolMemberUseCase
    )

    @Provides
    @FeatureScope
    fun provideUnbondingsInteractor(
        nominationPoolSharedComputation: NominationPoolSharedComputation,
        stakingSharedComputation: StakingSharedComputation,
    ): NominationPoolUnbondingsInteractor = RealNominationPoolUnbondingsInteractor(
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        stakingSharedComputation = stakingSharedComputation,
    )

    @Provides
    @FeatureScope
    fun provideStakeSummaryInteractor(
        stakingSharedComputation: StakingSharedComputation,
        poolAccountDerivation: PoolAccountDerivation,
        nominationPoolSharedComputation: NominationPoolSharedComputation,
    ): NominationPoolStakeSummaryInteractor = RealNominationPoolStakeSummaryInteractor(
        stakingSharedComputation = stakingSharedComputation,
        poolAccountDerivation = poolAccountDerivation,
        nominationPoolSharedComputation = nominationPoolSharedComputation
    )

    @Provides
    @FeatureScope
    fun provideNominationPoolRewardCalculatorFactory(
        stakingSharedComputation: StakingSharedComputation,
        poolAccountDerivation: PoolAccountDerivation,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository,
        nominationPoolStateRepository: NominationPoolStateRepository,
    ): NominationPoolRewardCalculatorFactory {
        return NominationPoolRewardCalculatorFactory(
            sharedStakingSharedComputation = stakingSharedComputation,
            poolAccountDerivation = poolAccountDerivation,
            nominationPoolGlobalsRepository = nominationPoolGlobalsRepository,
            nominationPoolStateRepository = nominationPoolStateRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideNominationPoolSharedComputation(
        computationalCache: ComputationalCache,
        nominationPoolMemberUseCase: NominationPoolMemberUseCase,
        nominationPoolStateRepository: NominationPoolStateRepository,
        nominationPoolUnbondRepository: NominationPoolUnbondRepository,
        poolAccountDerivation: PoolAccountDerivation,
        nominationPoolRewardCalculatorFactory: NominationPoolRewardCalculatorFactory,
        nominationPoolGlobalsRepository: NominationPoolGlobalsRepository
    ): NominationPoolSharedComputation {
        return NominationPoolSharedComputation(
            computationalCache = computationalCache,
            nominationPoolMemberUseCase = nominationPoolMemberUseCase,
            nominationPoolStateRepository = nominationPoolStateRepository,
            nominationPoolUnbondRepository = nominationPoolUnbondRepository,
            poolAccountDerivation = poolAccountDerivation,
            nominationPoolRewardCalculatorFactory = nominationPoolRewardCalculatorFactory,
            nominationPoolGlobalsRepository = nominationPoolGlobalsRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideUserRewardsInteractor(
        repository: NominationPoolMembersRepository,
        stakingRewardsRepository: StakingRewardsRepository,
    ): NominationPoolsUserRewardsInteractor = RealNominationPoolsUserRewardsInteractor(repository, stakingRewardsRepository)

    @Provides
    @FeatureScope
    fun provideYourPoolInteractor(
        poolAccountDerivation: PoolAccountDerivation,
        poolStateRepository: NominationPoolStateRepository,
    ): NominationPoolYourPoolInteractor = RealNominationPoolYourPoolInteractor(poolAccountDerivation, poolStateRepository)

    @Provides
    @FeatureScope
    fun provideAlertsInteractor(
        nominationPoolsSharedComputation: NominationPoolSharedComputation,
        stakingSharedComputation: StakingSharedComputation,
        poolAccountDerivation: PoolAccountDerivation,
    ): NominationPoolsAlertsInteractor = RealNominationPoolsAlertsInteractor(
        nominationPoolsSharedComputation = nominationPoolsSharedComputation,
        stakingSharedComputation = stakingSharedComputation,
        poolAccountDerivation = poolAccountDerivation
    )

    @Provides
    @FeatureScope
    fun provideHintsUseCase(
        stakingSharedState: StakingSharedState,
        poolMembersRepository: NominationPoolMembersRepository,
        accountRepository: AccountRepository,
        resourceManager: ResourceManager,
    ): NominationPoolHintsUseCase = RealNominationPoolHintsUseCase(
        stakingSharedState = stakingSharedState,
        poolMembersRepository = poolMembersRepository,
        accountRepository = accountRepository,
        resourceManager = resourceManager
    )
}
