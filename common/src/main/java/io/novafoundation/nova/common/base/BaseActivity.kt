package io.novafoundation.nova.common.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.viewbinding.ViewBinding
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.utils.showToast
import javax.inject.Inject

abstract class BaseActivity<T : BaseViewModel, B : ViewBinding> :
    AppCompatActivity(), BaseScreenMixin<T> {

    override val providedContext: Context
        get() = this

    override val lifecycleOwner: LifecycleOwner
        get() = this

    protected lateinit var binder: B
        private set

    @Inject
    override lateinit var viewModel: T

    protected abstract fun createBinding(): B

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

        binder = createBinding()

        setContentView(binder.root)

        inject()
        initViews()
        subscribe(viewModel)

        viewModel.errorLiveData.observeEvent(::showError)

        viewModel.messageLiveData.observeEvent(::showMessage)

        viewModel.toastLiveData.observeEvent { showToast(it) }
    }

    abstract fun changeLanguage()
}
