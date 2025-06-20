package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import io.novafoundation.nova.feature_account_api.data.externalAccounts.ExternalAccountsSyncService
import io.novafoundation.nova.feature_account_impl.data.sync.ExternalAccountsSyncDataSource
import io.novafoundation.nova.feature_account_impl.data.sync.MultisigAccountsSyncDataSourceFactory
import io.novafoundation.nova.feature_account_impl.data.sync.ProxyAccountsSyncDataSourceFactory
import io.novafoundation.nova.feature_account_impl.data.sync.RealExternalAccountsSyncService
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
        fun bindsExternalSyncService(real: RealExternalAccountsSyncService): ExternalAccountsSyncService
    }
}
