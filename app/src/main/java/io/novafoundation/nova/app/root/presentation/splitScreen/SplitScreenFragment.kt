package io.novafoundation.nova.app.root.presentation.splitScreen

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.RoundCornersOutlineProvider
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_dapp_impl.presentation.tab.setupCloseAllDappTabsDialogue
import java.io.File
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPoint
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointClose
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointIcon
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointText
import kotlinx.android.synthetic.main.fragment_split_screen.mainNavHost

class SplitScreenFragment : BaseFragment<SplitScreenViewModel>() {

    @Inject
    lateinit var mainNavigationHolder: MainNavigationHolder

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_split_screen, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainNavigationHolder.attach(mainNavController)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        mainNavigationHolder.detach()
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .splitScreenFragmentComponentFactory()
            .create(this)
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
            dappEntryPointIcon.letOrHide(model.iconPath) {
                dappEntryPointIcon.load(File(it), imageLoader)
            }
            dappEntryPointText.text = model.title
        }
    }

    private val mainNavController: NavController by lazy {
        val navHostFragment = childFragmentManager.findFragmentById(R.id.mainNavHost) as NavHostFragment

        navHostFragment.navController
    }

    private fun getOutlineCornerRadius(isRounded: Boolean): Float {
        return if (isRounded) {
            12f
        } else {
            0f
        }
    }
}
