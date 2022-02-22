package io.novafoundation.nova.feature_nft_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_nft_api.data.repository.NftRepository
import io.novafoundation.nova.feature_nft_impl.data.repository.NftRepositoryImpl

@Module
class NftFeatureModule {

    @Provides
    @FeatureScope
    fun provideNftRepository(): NftRepository = NftRepositoryImpl()
}
