package io.novafoundation.nova.feature_xcm_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_xcm_impl.config.api.XcmConfigApi
import io.novafoundation.nova.feature_xcm_impl.di.modules.BindsModule

@Module(
    includes = [
        BindsModule::class
    ]
)
class XcmFeatureModule {

    @FeatureScope
    @Provides
    fun provideXcmConfigApi(apiCreator: NetworkApiCreator): XcmConfigApi {
        return apiCreator.create(XcmConfigApi::class.java)
    }
}
