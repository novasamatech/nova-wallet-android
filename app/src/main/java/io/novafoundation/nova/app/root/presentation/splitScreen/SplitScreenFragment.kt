package io.novafoundation.nova.app.root.presentation.splitScreen

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.app.R
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
import java.io.File
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPoint
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointClose
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointIcon
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointText
import kotlinx.android.synthetic.main.fragment_split_screen.mainNavHost

class SplitScreenFragment : BaseFragment<SplitScreenViewModel>() {

    companion object : PayloadCreator<SplitScreenPayload> by FragmentPayloadCreator()

    @Inject
    lateinit var splitScreenNavigationHolder: SplitScreenNavigationHolder

    @Inject
    lateinit var imageLoader: ImageLoader

    private val mainNavController: NavController by lazy {
        val navHostFragment = childFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment

        navHostFragment.navController
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_split_screen, container, false)
    }

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
        mainNavHost.outlineProvider = RoundCornersOutlineProvider(12.dpF, margin = outlineMargin)

        dappEntryPoint.setOnClickListener { viewModel.onTabsClicked() }
        dappEntryPointClose.setOnClickListener { viewModel.onTabsCloseClicked() }
    }

    override fun subscribe(viewModel: SplitScreenViewModel) {
        setupCloseAllDappTabsDialogue(viewModel.closeAllTabsConfirmation)

        viewModel.dappTabsVisible.observe { shouldBeVisible ->
            mainNavHost.clipToOutline = shouldBeVisible
            dappEntryPoint.isVisible = shouldBeVisible
        }

        viewModel.tabsTitle.observe { model ->
            dappEntryPointIcon.letOrHide(model.icon) {
                dappEntryPointIcon.setIcon(it, imageLoader)
            }
            dappEntryPointText.text = model.title
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
        ViewCompat.setOnApplyWindowInsetsListener(mainNavHost) { _, insets ->
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
            dappEntryPoint.post {
                dappEntryPointHeight = if (dappEntryPoint.isVisible) {
                    dappEntryPoint.height + dappEntryPoint.marginTop
                } else {
                    0
                }
                ViewCompat.requestApplyInsets(mainNavHost) // Request new insets to trigger inset listener
            }
        }
    }
}
