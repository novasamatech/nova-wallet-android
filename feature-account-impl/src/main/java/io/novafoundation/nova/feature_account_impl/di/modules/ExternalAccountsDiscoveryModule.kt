package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_impl.data.proxy.network.api.FindProxiesApi
import io.novafoundation.nova.feature_account_impl.data.proxy.repository.MultiChainProxyRepository
import io.novafoundation.nova.feature_account_impl.data.proxy.repository.RealMultiChainProxyRepository
import io.novafoundation.nova.feature_account_impl.data.sync.DerivativeAccountsSyncDataSourceFactory
import io.novafoundation.nova.feature_account_impl.data.sync.ExternalAccountsSyncDataSource
import io.novafoundation.nova.feature_account_impl.data.sync.MultisigAccountsSyncDataSourceFactory
import io.novafoundation.nova.feature_account_impl.data.sync.ProxyAccountsSyncDataSourceFactory
import io.novafoundation.nova.feature_account_impl.data.sync.RealExternalAccountsSyncService
import io.novafoundation.nova.feature_account_impl.data.sync.common.DelegatedAccountCreator
import io.novafoundation.nova.feature_account_impl.data.sync.common.RealDelegatedAccountCreator
import io.novafoundation.nova.feature_account_impl.di.modules.ExternalAccountsDiscoveryModule.BindsModule

@Module(includes = [BindsModule::class])
class ExternalAccountsDiscoveryModule {

    @Module
    internal interface BindsModule {

        @Binds
        @IntoSet
        fun bindProxySyncSourceFactoryToSet(real: ProxyAccountsSyncDataSourceFactory): ExternalAccountsSyncDataSource.Factory

        @Binds
        @IntoSet
        fun bindMultisigSyncSourceFactoryToSet(real: MultisigAccountsSyncDataSourceFactory): ExternalAccountsSyncDataSource.Factory

        @Binds
        @IntoSet
        fun bindDerivativeSyncSourceFactoryToSet(real: DerivativeAccountsSyncDataSourceFactory): ExternalAccountsSyncDataSource.Factory

        @Binds
        fun bindsExternalSyncService(real: RealExternalAccountsSyncService): ExternalAccountsSyncService

        @Binds
        fun bindMultiChainProxyRepository(real: RealMultiChainProxyRepository): MultiChainProxyRepository

        @Binds
        fun bindDelegatedAccountCreator(real: RealDelegatedAccountCreator): DelegatedAccountCreator
    }

    @Provides
    @FeatureScope
    fun provideMultiChainProxyApi(apiCreator: NetworkApiCreator): FindProxiesApi {
        return apiCreator.create(FindProxiesApi::class.java)
    }
}
