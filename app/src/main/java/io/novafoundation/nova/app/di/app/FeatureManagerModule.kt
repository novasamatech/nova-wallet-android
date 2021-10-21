package io.novafoundation.nova.app.di.app

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.di.deps.FeatureHolderManager
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.scope.ApplicationScope

@Module
class FeatureManagerModule {

    @ApplicationScope
    @Provides
    fun provideFeatureHolderManager(featureApiHolderMap: @JvmSuppressWildcards Map<Class<*>, FeatureApiHolder>): FeatureHolderManager {
        return FeatureHolderManager(featureApiHolderMap)
    }
}
