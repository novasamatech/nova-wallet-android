package io.novafoundation.nova.app.root.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.app.root.presentation.common.FirebaseServiceInitializer
import io.novafoundation.nova.analytics.AnalyticsOptOutManager
import io.novafoundation.nova.analytics.AnalyticsService
import io.novafoundation.nova.analytics.transport.AnalyticsLifecycleInitializer
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coroutines.DangerousScope
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.interfaces.CompoundExternalServiceInitializer
import io.novafoundation.nova.common.interfaces.ExternalServiceInitializer

@Module
class ExternalServiceInitializersModule {

    @Provides
    @IntoSet
    fun provideFirebaseServiceInitializer(
        context: Context
    ): ExternalServiceInitializer {
        return FirebaseServiceInitializer(context)
    }

    @OptIn(DangerousScope::class)
    @Provides
    @IntoSet
    fun provideAnalyticsLifecycleInitializer(
        rootScope: RootScope,
        analyticsService: AnalyticsService,
        optOutManager: AnalyticsOptOutManager
    ): ExternalServiceInitializer {
        return AnalyticsLifecycleInitializer(rootScope, analyticsService, optOutManager)
    }

    @Provides
    @FeatureScope
    fun provideCompoundExternalServiceInitializer(
        initializers: Set<@JvmSuppressWildcards ExternalServiceInitializer>
    ): ExternalServiceInitializer {
        return CompoundExternalServiceInitializer(initializers)
    }
}
