package io.novafoundation.nova.app.root.presentation.main

import android.content.res.ColorStateList
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

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
        stakingDashboardNavigator.setStakingTabNavController(navController!!)

        binder.bottomNavigationView.setupWithNavController(navController!!)
        binder.bottomNavigationView.itemIconTintList = null
        binder.bottomNavigationView.setupNewYearItemColors(navController!!)

        requireActivity().onBackPressedDispatcher.addCallback(backCallback)

        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            backCallback.isEnabled = !isAtHomeTab(destination)
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .mainFragmentComponentFactory()
            .create(requireActivity())
            .inject(this)
    }

    override fun subscribe(viewModel: MainViewModel) {}

    private fun isAtHomeTab(destination: NavDestination) =
        destination.id == navController!!.graph.startDestination
}

private fun BottomNavigationView.setupNewYearItemColors(navController: NavController) {
    val destinationIdToColor = mapOf(
        R.id.walletFragment to context.getColor(R.color.new_year_assets),
        R.id.voteFragment to context.getColor(R.color.new_year_vote),
        R.id.dAppsFragment to context.getColor(R.color.new_year_dapps),
        R.id.stakingDashboardFragment to context.getColor(R.color.new_year_staking),
        R.id.moreStakingOptionsFragment to context.getColor(R.color.new_year_staking),
        R.id.profileFragment to context.getColor(R.color.new_year_settings)
    )

    val inactiveColor = context.getColor(R.color.nav_bar_icon_inactive)

    val states = arrayOf(
        intArrayOf(android.R.attr.state_checked),
        intArrayOf()
    )

    navController.addOnDestinationChangedListener { _, destination, _ ->
        val colors = intArrayOf(
            destinationIdToColor[destination.id] ?: inactiveColor,
            inactiveColor
        )

        val dynamicColorList = ColorStateList(states, colors)

        itemTextColor = dynamicColorList
        itemIconTintList = dynamicColorList
    }
}
