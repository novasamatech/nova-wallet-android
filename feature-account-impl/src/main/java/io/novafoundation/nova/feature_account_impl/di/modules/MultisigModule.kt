package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigDiscoveryService
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.RealMultisigDiscoveryService
import io.novafoundation.nova.feature_account_impl.data.multisig.RealMultisigRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.api.FindMultisigsApi
import io.novafoundation.nova.feature_account_impl.di.modules.MultisigModule.BindsModule
import io.novafoundation.nova.feature_account_impl.domain.multisig.RealMultisigPendingOperationsService

@Module(includes = [BindsModule::class])
class MultisigModule {

    @Module
    internal interface BindsModule {

        @Binds
        fun bindMultisigSyncService(real: RealMultisigDiscoveryService): MultisigDiscoveryService

        @Binds
        fun bindMultisigSyncRepository(real: RealMultisigRepository): MultisigRepository

        @Binds
        fun bindMultisigPendingOperationsService(real: RealMultisigPendingOperationsService): MultisigPendingOperationsService
    }

    @Provides
    @FeatureScope
    fun provideFindMultisigsApi(apiCreator: NetworkApiCreator): FindMultisigsApi {
        return apiCreator.create(FindMultisigsApi::class.java)
    }
}
