package io.novafoundation.nova.common.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import io.novafoundation.nova.common.di.FeatureContainer
import javax.inject.Inject

abstract class BaseActivity<T : BaseViewModel> :
    AppCompatActivity(), BaseScreenMixin<T> {

    override val providedContext: Context
        get() = this

    override val lifecycleOwner: LifecycleOwner
        get() = this

    @Inject
    override lateinit var viewModel: T

    override fun attachBaseContext(base: Context) {
        val commonApi = (base.applicationContext as FeatureContainer).commonApi()
        val contextManager = commonApi.contextManager()
        applyOverrideConfiguration(contextManager.setLocale(base).resources.configuration)
        super.attachBaseContext(contextManager.setLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val decorView = window.decorView
        decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )

        setContentView(layoutResource())

        inject()
        initViews()
        subscribe(viewModel)

        viewModel.errorLiveData.observeEvent(::showError)

        viewModel.messageLiveData.observeEvent(::showMessage)
    }

    abstract fun layoutResource(): Int

    abstract fun changeLanguage()
}
