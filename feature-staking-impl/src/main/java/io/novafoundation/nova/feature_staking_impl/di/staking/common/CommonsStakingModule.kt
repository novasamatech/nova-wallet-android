package io.novafoundation.nova.feature_staking_impl.di.staking.common

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_staking_impl.data.common.repository.CommonStakingRepository
import io.novafoundation.nova.feature_staking_impl.data.common.repository.RealCommonStakingRepository
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class CommonsStakingModule {

    @Provides
    @FeatureScope
    fun provideCommonStakingRepository(
        @Named(LOCAL_STORAGE_SOURCE) storageDataSource: StorageDataSource
    ): CommonStakingRepository = RealCommonStakingRepository(storageDataSource)
}
