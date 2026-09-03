package io.novafoundation.nova.app.root.presentation.main

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

import io.novafoundation.nova.common.R as CommonR
import io.novafoundation.nova.common.view.dialog.dialog
import io.novafoundation.nova.common.utils.observeEvent
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.FragmentMainBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.navigators.staking.StakingDashboardNavigator
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils

import javax.inject.Inject

class MainFragment : BaseFragment<MainViewModel, FragmentMainBinding>() {

    override fun createBinding() = FragmentMainBinding.inflate(layoutInflater)

    @Inject
    lateinit var stakingDashboardNavigator: StakingDashboardNavigator

    private var navController: NavController? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = navController!!.navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        backCallback.isEnabled = false
        stakingDashboardNavigator.clearStakingTabNavController()
    }

    override fun applyInsets(rootView: View) {
        // Bottom Navigation View apply insets by itself so we override it to do nothing
    }

    override fun initViews() {
        val nestedNavHostFragment = childFragmentManager.findFragmentById(R.id.bottomNavHost) as NavHostFragment

        navController = nestedNavHostFragment.navController

        binder.bottomNavigationView.setupWithNavController(navController!!)
        binder.bottomNavigationView.itemIconTintList = null

        stakingDashboardNavigator.setStakingTabNavController(navController!!)

        requireActivity().onBackPressedDispatcher.addCallback(backCallback)

        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            backCallback.isEnabled = !isAtHomeTab(destination)

            viewModel.onDestinationChanged(destination.id)
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .mainFragmentComponentFactory()
            .create(requireActivity())
            .inject(this)
    }

    override fun subscribe(viewModel: MainViewModel) {
        viewModel.showAnalyticsConsent.observeEvent {
            showAnalyticsConsentDialog()
        }
    }

    private fun showAnalyticsConsentDialog() {
        dialog(requireContext()) {
            setTitle(CommonR.string.analytics_prompt_title)
            setMessage(CommonR.string.analytics_prompt_message)
            setPositiveButton(CommonR.string.analytics_prompt_enable) { _, _ ->
                viewModel.onAnalyticsConsentGiven()
            }
            setNegativeButton(CommonR.string.analytics_prompt_decline) { _, _ ->
                viewModel.onAnalyticsConsentDeclined()
            }
        }
    }

    private fun isAtHomeTab(destination: NavDestination) =
        destination.id == navController!!.graph.startDestination
}
