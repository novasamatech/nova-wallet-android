package io.novafoundation.nova.app.di.app

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.app.App
import io.novafoundation.nova.app.di.app.modules.BrowserTabsModule
import io.novafoundation.nova.app.di.app.navigation.NavigationModule
import io.novafoundation.nova.app.di.deps.ComponentHolderModule
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.common.di.modules.NetworkModule
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.coroutines.RootScope

@ApplicationScope
@Component(
    modules = [
        AppModule::class,
        CommonModule::class,
        NetworkModule::class,
        NavigationModule::class,
        ComponentHolderModule::class,
        FeatureManagerModule::class,
        BrowserTabsModule::class
    ]
)
interface AppComponent : CommonApi {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: App): Builder

        @BindsInstance
        fun contextManager(contextManager: ContextManager): Builder

        @BindsInstance
        fun rootScope(rootScope: RootScope): Builder

        fun build(): AppComponent
    }

    fun inject(app: App)
}
