package io.novafoundation.nova.feature_proxy_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_impl.data.common.RealProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_impl.data.repository.RealGetProxyRepository
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class ProxyFeatureModule {

    @Provides
    @FeatureScope
    fun provideProxyRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteSource: StorageDataSource,
        @Named(LOCAL_STORAGE_SOURCE) localSource: StorageDataSource,
        chainRegistry: ChainRegistry
    ): GetProxyRepository = RealGetProxyRepository(
        remoteSource = remoteSource,
        localSource = localSource,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideProxyDepositCalculator(chainRegistry: ChainRegistry): ProxyDepositCalculator {
        return RealProxyDepositCalculator(chainRegistry)
    }
}
