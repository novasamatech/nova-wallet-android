package io.novafoundation.nova.app.root.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.ActivityRootBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.NavigationHolder
import io.novafoundation.nova.common.base.BaseActivity
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.utils.EventObserver
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.showToast
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.view.bottomSheet.action.observeActionBottomSheet
import io.novafoundation.nova.splash.presentation.SplashBackgroundHolder

import javax.inject.Inject

class RootActivity : BaseActivity<RootViewModel, ActivityRootBinding>(), SplashBackgroundHolder {

    @Inject
    lateinit var navigationHolder: NavigationHolder

    @Inject
    lateinit var systemCallExecutor: SystemCallExecutor

    @Inject
    lateinit var contextManager: ContextManager

    override fun createBinding(): ActivityRootBinding {
        return ActivityRootBinding.inflate(LayoutInflater.from(this))
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .mainActivityComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        removeSplashBackground()

        viewModel.restoredAfterConfigChange()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!systemCallExecutor.onActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navigationHolder.attach(navController)
        contextManager.attachActivity(this)

        binder.rootNetworkBar.setOnApplyWindowInsetsListener { view, insets ->
            view.updatePadding(top = insets.systemWindowInsetTop)

            insets
        }

        intent?.let(::processIntent)

        viewModel.applySafeModeIfEnabled()
//        processJsonOpenIntent()
    }

    override fun onDestroy() {
        super.onDestroy()

        contextManager.detachActivity()
        navigationHolder.detach()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        processIntent(intent)
    }

    override fun initViews() {
    }

    override fun onStop() {
        super.onStop()

        viewModel.noticeInBackground()
    }

    override fun onStart() {
        super.onStart()

        viewModel.noticeInForeground()
    }

    override fun subscribe(viewModel: RootViewModel) {
        observeActionBottomSheet(viewModel)

        viewModel.showConnectingBarLiveData.observe(this) { show ->
            binder.rootNetworkBar.setVisible(show)
        }

        viewModel.messageLiveData.observe(
            this,
            EventObserver {
                showToast(it)
            }
        )
    }

    override fun removeSplashBackground() {
        window.setBackgroundDrawableResource(R.color.secondary_screen_background)
    }

    override fun changeLanguage() {
        viewModel.noticeLanguageLanguage()

        recreate()
    }

    private fun processIntent(intent: Intent) {
        intent.data?.let {
            viewModel.handleDeepLink(it)
        }
    }

//    private fun processJsonOpenIntent() {
//        if (Intent.ACTION_VIEW == intent.action && intent.type != null) {
//            if ("application/json" == intent.type) {
//                val file = this.contentResolver.openInputStream(intent.data!!)
//                val content = file?.reader(Charsets.UTF_8)?.readText()
//                viewModel.jsonFileOpened(content)
//            }
//        }
//    }

    private val navController: NavController by lazy {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment

        navHostFragment.navController
    }
}
