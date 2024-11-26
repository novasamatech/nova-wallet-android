package io.novafoundation.nova.app.root.presentation.splitScreen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.holders.MainNavigationHolder
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_dapp_impl.presentation.tab.setupCloseAllDappTabsDialogue
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPoint
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointClose
import kotlinx.android.synthetic.main.fragment_split_screen.dappEntryPointText
import kotlinx.android.synthetic.main.fragment_split_screen.mainNavHost

class SplitScreenFragment : BaseFragment<SplitScreenViewModel>() {

    @Inject
    lateinit var mainNavigationHolder: MainNavigationHolder

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
        dappEntryPoint.setOnClickListener { viewModel.onTabsClicked() }
        dappEntryPointClose.setOnClickListener { viewModel.onTabsCloseClicked() }
    }

    override fun subscribe(viewModel: SplitScreenViewModel) {
        setupCloseAllDappTabsDialogue(viewModel.closeAllTabsConfirmation)

        viewModel.dappTabsQuantity.observe {
            val shouldBeVisible = it > 0
            val isVisibilityChanged = dappEntryPoint.isVisible != shouldBeVisible

            if (isVisibilityChanged) {
                mainNavHost.outlineProvider = RoundCornersOutlineProvider(getOutlineCornerRadius(shouldBeVisible))
            }

            dappEntryPoint.isVisible = it > 0
            dappEntryPointText.text = getString(R.string.dapp_entry_point_title, it)
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
