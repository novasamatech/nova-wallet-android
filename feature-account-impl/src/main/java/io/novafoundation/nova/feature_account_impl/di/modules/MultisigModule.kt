package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Binds
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigSyncService
import io.novafoundation.nova.feature_account_impl.data.multisig.MultisigSyncRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.RealMultisigSyncRepository
import io.novafoundation.nova.feature_account_impl.data.multisig.RealMultisigSyncService
import io.novafoundation.nova.feature_account_impl.data.multisig.api.FindMultisigsApi
import io.novafoundation.nova.feature_account_impl.di.modules.MultisigModule.BindsModule

@Module(includes = [BindsModule::class])
class MultisigModule {

    @Module
    internal interface BindsModule {

        @Binds
        fun bindMultisigSyncService(real: RealMultisigSyncService): MultisigSyncService

        @Binds
        fun bindMultisigSyncRepository(real: RealMultisigSyncRepository): MultisigSyncRepository
    }

    @Provides
    @FeatureScope
    fun provideFindMultisigsApi(apiCreator: NetworkApiCreator): FindMultisigsApi {
        return apiCreator.create(FindMultisigsApi::class.java)
    }
}
