package io.novafoundation.nova.app.root.presentation.splitScreen

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        manageImeInsets()
    }

    /**
     * Since we have a dAppEntryPoint we must change ime insets for main container and its children
     * to avoid extra bottom space when keyboard is shown
     */
    private fun manageImeInsets() {
        // We change this value when dappEntryPoint is shown/hidden
        var dappEntryPointHeight = 0

        // Inset listener that provides a custom insets to its children
        ViewCompat.setOnApplyWindowInsetsListener(binder.mainNavHost) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            val changedImeInsets = Insets.of(
                imeInsets.left,
                imeInsets.top,
                imeInsets.right,
                (imeInsets.bottom - dappEntryPointHeight).coerceAtLeast(0)
            )

            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.ime(), changedImeInsets)
                .build()
        }

        // Subscribe to get dAppEntryPoint height
        viewModel.dappTabsVisible.observe {
            binder.dappEntryPoint.post {
                dappEntryPointHeight = if (binder.dappEntryPoint.isVisible) {
                    binder.dappEntryPoint.height + binder.dappEntryPoint.marginTop
                } else {
                    0
                }
                ViewCompat.requestApplyInsets(binder.mainNavHost) // Request new insets to trigger inset listener
            }
        }
    }
}
