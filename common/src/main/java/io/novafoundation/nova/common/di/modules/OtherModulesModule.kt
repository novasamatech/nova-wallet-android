package io.novafoundation.nova.common.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.otherModules.HandleDeeplinkEventBus

@Module
class OtherModulesModule {

    @Provides
    @FeatureScope
    fun provideHandleDeeplinkEventBus() = HandleDeeplinkEventBus()
}
