package io.novafoundation.nova.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import io.novafoundation.nova.app.di.app.AppComponent
import io.novafoundation.nova.app.di.deps.FeatureHolderManager
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import javax.inject.Inject

open class App : Application(), FeatureContainer {

    @Inject lateinit var featureHolderManager: FeatureHolderManager

    private lateinit var appComponent: AppComponent

    private val languagesHolder: LanguagesHolder = LanguagesHolder()

    override fun attachBaseContext(base: Context) {
        val contextManager = ContextManager.getInstanceOrInit(base, languagesHolder)
        super.attachBaseContext(contextManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val contextManager = ContextManager.getInstanceOrInit(this, languagesHolder)
        contextManager.setLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        val contextManger = ContextManager.getInstanceOrInit(this, languagesHolder)

        appComponent = io.novafoundation.nova.app.di.app.DaggerAppComponent
            .builder()
            .application(this)
            .contextManager(contextManger)
            .build()

        appComponent.inject(this)
    }

    override fun <T> getFeature(key: Class<*>): T {
        return featureHolderManager.getFeature<T>(key)!!
    }

    override fun releaseFeature(key: Class<*>) {
        featureHolderManager.releaseFeature(key)
    }

    override fun commonApi(): CommonApi {
        return appComponent
    }
}
