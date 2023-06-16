package io.novafoundation.nova.feature_staking_impl.di.staking.nominationPool

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.RealPoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolBalanceRepository
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.RealNominationPoolBalanceRepository
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
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
    fun provideNominationPoolBalanceRepository(
        poolAccountDerivation: PoolAccountDerivation
    ): NominationPoolBalanceRepository = RealNominationPoolBalanceRepository(poolAccountDerivation)
}
