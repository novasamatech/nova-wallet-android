package io.novafoundation.nova.app.di.app

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.app.App
import io.novafoundation.nova.common.di.scope.ApplicationScope

@Module
class AppModule {

    @ApplicationScope
    @Provides
    fun provideContext(application: App): Context {
        return application
    }
}
