package io.novafoundation.nova.app.root.presentation.main

import android.view.View
import android.os.Bundle
import android.os.Build
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.FragmentMainBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.navigators.staking.StakingDashboardNavigator
import io.novafoundation.nova.app.root.navigation.navigators.swap.SwapTabNavigator
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils

import javax.inject.Inject

class MainFragment : BaseFragment<MainViewModel, FragmentMainBinding>() {

    @Inject
    lateinit var stakingDashboardNavigator: StakingDashboardNavigator

    @Inject
    lateinit var swapTabNavigator: SwapTabNavigator

    private var navController: NavController? = null

    private var moreNavigationPopup: MoreNavigationPopup? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = navController!!.navigateUp()
        }
    }

    private val destinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        moreNavigationPopup?.dismiss()
        backCallback.isEnabled = !isAtHomeTab(destination)

        if (selectedMoreDestination(destination) != null) {
            binder.bottomNavigationView.menu.findItem(R.id.moreNavigation).isChecked = true
        }

        viewModel.onDestinationChanged(destination.id)
        ViewCompat.requestApplyInsets(binder.root)
    }

    override fun createBinding() = FragmentMainBinding.inflate(layoutInflater)

    override fun onPause() {
        moreNavigationPopup?.dismiss()
        super.onPause()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        swapTabNavigator.attach(requireNotNull(navController))
    }

    override fun onDestroyView() {
        moreNavigationPopup?.dismiss()
        moreNavigationPopup = null
        navController?.removeOnDestinationChangedListener(destinationChangedListener)
        navController = null
        backCallback.isEnabled = false
        stakingDashboardNavigator.clearStakingTabNavController()
        swapTabNavigator.detach()

        super.onDestroyView()
    }

    override fun applyInsets(rootView: View) {
        val initialBottomPadding = rootView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val swapKeyboardVisible = navController?.currentDestination?.id == R.id.swapsTabFragment &&
                insets.isVisible(WindowInsetsCompat.Type.ime())

            binder.bottomNavigationView.isVisible = !swapKeyboardVisible
            binder.view2.isVisible = !swapKeyboardVisible
            rootView.updatePadding(
                bottom = initialBottomPadding + if (swapKeyboardVisible) insets.getInsets(WindowInsetsCompat.Type.ime()).bottom else 0
            )
            if (swapKeyboardVisible) moreNavigationPopup?.dismiss()

            // Keep BaseFragment's inset dispatch for nested fragments on Android 7–10.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                binder.root.children.forEach { ViewCompat.dispatchApplyWindowInsets(it, insets) }
            }
            insets
        }
    }

    override fun initViews() {
        val nestedNavHostFragment = childFragmentManager.findFragmentById(R.id.bottomNavHost) as NavHostFragment

        navController = nestedNavHostFragment.navController

        binder.bottomNavigationView.setupWithNavController(navController!!)
        binder.bottomNavigationView.itemIconTintList = null
        binder.bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            if (item.itemId == R.id.moreNavigation) {
                showMoreNavigation()
                false
            } else {
                moreNavigationPopup?.dismiss()
                NavigationUI.onNavDestinationSelected(item, navController!!)
            }
        }

        stakingDashboardNavigator.setStakingTabNavController(navController!!)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
        navController!!.addOnDestinationChangedListener(destinationChangedListener)
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .mainFragmentComponentFactory()
            .create(requireActivity())
            .inject(this)
    }

    override fun subscribe(viewModel: MainViewModel) {
        // Nothing to observe: analytics consent is its own screen now
    }

    private fun showMoreNavigation() {
        if (moreNavigationPopup?.isShowing == true) {
            moreNavigationPopup?.dismiss()
            return
        }

        moreNavigationPopup = MoreNavigationPopup(
            navigationBar = binder.bottomNavigationView,
            selectedDestinationId = selectedMoreDestination(navController?.currentDestination),
            onDestinationSelected = ::openMoreDestination
        ).also { popup ->
            popup.setOnDismissListener { moreNavigationPopup = null }
            popup.show()
        }
    }

    private fun openMoreDestination(destinationId: Int) {
        val controller = navController ?: return
        if (controller.currentDestination?.id == destinationId) return

        // Match the existing bottom-tab navigation: one destination above Assets, without duplicates.
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(controller.graph.startDestination, false)
            .setEnterAnim(R.animator.nav_default_enter_anim)
            .setExitAnim(R.animator.nav_default_exit_anim)
            .setPopEnterAnim(R.animator.nav_default_pop_enter_anim)
            .setPopExitAnim(R.animator.nav_default_pop_exit_anim)
            .build()
        controller.navigate(destinationId, null, options)
    }

    private fun selectedMoreDestination(destination: NavDestination?): Int? {
        return generateSequence(destination) { it.parent }
            .firstOrNull { it.id == R.id.voteFragment || it.id == R.id.profileFragment }
            ?.id
    }

    private fun isAtHomeTab(destination: NavDestination) =
        destination.id == navController!!.graph.startDestination
}
