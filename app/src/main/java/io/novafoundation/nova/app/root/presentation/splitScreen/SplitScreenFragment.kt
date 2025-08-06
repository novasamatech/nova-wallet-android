package io.novafoundation.nova.app.root.presentation.splitScreen

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.ImageLoader
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.FragmentSplitScreenBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.holders.SplitScreenNavigationHolder
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.RoundCornersOutlineProvider
import io.novafoundation.nova.common.utils.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.payloadOrElse
import io.novafoundation.nova.feature_dapp_impl.presentation.tab.setupCloseAllDappTabsDialogue
import javax.inject.Inject

class SplitScreenFragment : BaseFragment<SplitScreenViewModel, FragmentSplitScreenBinding>() {

    companion object : PayloadCreator<SplitScreenPayload> by FragmentPayloadCreator()

    @Inject
    lateinit var splitScreenNavigationHolder: SplitScreenNavigationHolder

    @Inject
    lateinit var imageLoader: ImageLoader

    private val mainNavController: NavController by lazy {
        val navHostFragment = childFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment

        navHostFragment.navController
    }

    override fun applyInsets(rootView: View) {
        // Implemented to not consume insets for nested fragments
    }

    override fun createBinding() = FragmentSplitScreenBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        splitScreenNavigationHolder.attach(mainNavController)

        viewModel.onNavigationAttached()
    }

    override fun onDestroyView() {
        splitScreenNavigationHolder.detachNavController(mainNavController)

        super.onDestroyView()
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .splitScreenFragmentComponentFactory()
            .create(this, payloadOrElse { SplitScreenPayload.NoNavigation })
            .inject(this)
    }

    override fun initViews() {
        val outlineMargin = Rect(0, (-12).dp, 0, 0) // To avoid round corners at top
        binder.mainNavHost.outlineProvider = RoundCornersOutlineProvider(12.dpF, margin = outlineMargin)

        binder.dappEntryPoint.setOnClickListener { viewModel.onTabsClicked() }
        binder.dappEntryPointClose.setOnClickListener { viewModel.onTabsCloseClicked() }
    }

    override fun subscribe(viewModel: SplitScreenViewModel) {
        setupCloseAllDappTabsDialogue(viewModel.closeAllTabsConfirmation)

        viewModel.dappTabsVisible.observe { shouldBeVisible ->
            binder.mainNavHost.clipToOutline = shouldBeVisible
            binder.dappEntryPoint.isVisible = shouldBeVisible
        }

        viewModel.tabsTitle.observe { model ->
            binder.dappEntryPointIcon.letOrHide(model.icon) {
                binder.dappEntryPointIcon.setIcon(it, imageLoader)
            }
            binder.dappEntryPointText.text = model.title
        }
        manageInsets()
    }

    /**
     * Since we have a dAppEntryPoint we must change ime insets for main container and its children
     * to avoid extra bottom space when keyboard is shown
     */
    private fun manageInsets() {
        binder.dappEntryPoint.applyNavigationBarInsets()

        var dappEntryPointShown = false
        var dappEntryPointHeight = 0

        // Inset listener that provides a custom insets to its children
        ViewCompat.setOnApplyWindowInsetsListener(binder.mainNavHost) { _, insets ->
            val insetsBuilder = WindowInsetsCompat.Builder(insets)
            // We need to remove height from ime inset to don't show dapp entry point when keyboard is shown
            insetsBuilder.setInsets(Type.ime(), insets.getInsets(Type.ime()).withoutBottom(dappEntryPointHeight))

            // We also remove other navigation ans gestures insets since dappEntryPoint must use them instead of any nested fragment
            insetsBuilder.setInsets(Type.navigationBars(), insets.getInsets(Type.navigationBars()).removeBottom(dappEntryPointShown))
            insetsBuilder.setInsets(Type.systemGestures(), insets.getInsets(Type.systemGestures()).removeBottom(dappEntryPointShown))
            insetsBuilder.build()
        }

        // Subscribe to change insets when dappEntryPoint changes its visibility
        viewModel.dappTabsVisible.observe {
            // Change this instantly to avoid delays until dappEntryPoint will be measured
            dappEntryPointShown = it
            ViewCompat.requestApplyInsets(binder.mainNavHost)

            // Change this only after dappEntryPoint will be measured to setup a keyboard insets
            binder.dappEntryPoint.post {
                dappEntryPointHeight = if (binder.dappEntryPoint.isVisible) {
                    binder.dappEntryPoint.height + binder.dappEntryPoint.marginTop
                } else {
                    0
                }

                ViewCompat.requestApplyInsets(binder.mainNavHost)
            }
        }
    }
}
