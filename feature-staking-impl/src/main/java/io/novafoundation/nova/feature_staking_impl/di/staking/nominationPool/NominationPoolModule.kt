package io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
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
import io.novafoundation.nova.feature_staking_impl.data.repository.StakingRewardsRepository
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.common.StakingSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.RealNominationPoolMemberUseCase
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards.NominationPoolRewardCalculatorFactory
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
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
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
    fun provideNominationPoolGlobalsRepository(
        @Named(LOCAL_STORAGE_SOURCE) localStorageSource: StorageDataSource
    ): NominationPoolGlobalsRepository {
        return RealNominationPoolGlobalsRepository(localStorageSource)
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
        nominationPoolUnbondRepository: NominationPoolUnbondRepository,
        stakingSharedComputation: StakingSharedComputation,
    ): NominationPoolUnbondingsInteractor = RealNominationPoolUnbondingsInteractor(
        nominationPoolUnbondRepository = nominationPoolUnbondRepository,
        stakingSharedComputation = stakingSharedComputation,
    )

    @Provides
    @FeatureScope
    fun provideStakeSummaryInteractor(
        nominationPoolStateRepository: NominationPoolStateRepository,
        stakingSharedComputation: StakingSharedComputation,
        noPoolAccountDerivation: PoolAccountDerivation,
    ): NominationPoolStakeSummaryInteractor = RealNominationPoolStakeSummaryInteractor(
        nominationPoolStateRepository = nominationPoolStateRepository,
        stakingSharedComputation = stakingSharedComputation,
        noPoolAccountDerivation = noPoolAccountDerivation,
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
    ): NominationPoolSharedComputation {
        return NominationPoolSharedComputation(computationalCache, nominationPoolMemberUseCase)
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
}
