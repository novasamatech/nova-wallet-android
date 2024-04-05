package io.novafoundation.nova.app.di.app

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.App
import io.novafoundation.nova.app.root.presentation.common.RealBuildTypeProvider
import io.novafoundation.nova.app.root.presentation.common.RootActivityIntentProvider
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider
import io.novafoundation.nova.common.interfaces.BuildTypeProvider

@Module
class AppModule {

    @ApplicationScope
    @Provides
    fun provideContext(application: App): Context {
        return application
    }

    @Provides
    @ApplicationScope
    fun provideRootActivityIntentProvider(context: Context): ActivityIntentProvider = RootActivityIntentProvider(context)

    @Provides
    @ApplicationScope
    fun provideBuildTypeProvider(): BuildTypeProvider = RealBuildTypeProvider()
}
