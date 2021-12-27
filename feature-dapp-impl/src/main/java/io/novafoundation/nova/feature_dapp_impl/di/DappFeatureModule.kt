package io.novafoundation.nova.feature_dapp_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_dapp_impl.di.modules.Web3Module
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor

@Module(includes = [Web3Module::class])
class DappFeatureModule {

    @Provides
    @FeatureScope
    fun provideCommonInteractor() = DappInteractor()
}
