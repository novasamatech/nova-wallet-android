package io.novafoundation.nova.feature_proxy_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.ProxyConstantsRepository
import io.novafoundation.nova.feature_proxy_impl.data.common.RealProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_impl.data.repository.RealGetProxyRepository
import io.novafoundation.nova.feature_proxy_impl.data.repository.RealProxyConstantsRepository
import io.novafoundation.nova.runtime.di.LOCAL_STORAGE_SOURCE
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import javax.inject.Named

@Module
class ProxyFeatureModule {

    @Provides
    @FeatureScope
    fun provideProxyRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteSource: StorageDataSource,
        @Named(LOCAL_STORAGE_SOURCE) localSource: StorageDataSource,
        chainRegistry: ChainRegistry,
        rpcCalls: RpcCalls
    ): GetProxyRepository = RealGetProxyRepository(
        remoteSource = remoteSource,
        localSource = localSource,
        chainRegistry,
        rpcCalls
    )

    @Provides
    @FeatureScope
    fun provideProxyConstantsRepository(
        chainRegistry: ChainRegistry
    ): ProxyConstantsRepository = RealProxyConstantsRepository(
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideProxyDepositCalculator(
        chainRegistry: ChainRegistry,
        proxyConstantsRepository: ProxyConstantsRepository
    ): ProxyDepositCalculator {
        return RealProxyDepositCalculator(chainRegistry, proxyConstantsRepository)
    }
}
