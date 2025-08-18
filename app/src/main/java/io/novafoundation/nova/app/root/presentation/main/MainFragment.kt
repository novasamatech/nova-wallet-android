package io.novafoundation.nova.app.root.presentation.main

import android.graphics.RectF
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController

import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.databinding.FragmentMainBinding
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.navigators.staking.StakingDashboardNavigator
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.blur.SweetBlur
import io.novafoundation.nova.common.utils.setBackgroundColorRes

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

    override fun initViews() {
        val nestedNavHostFragment = childFragmentManager.findFragmentById(R.id.bottomNavHost) as NavHostFragment

        navController = nestedNavHostFragment.navController
        stakingDashboardNavigator.setStakingTabNavController(navController!!)

        binder.bottomNavigationView.setupWithNavController(navController!!)
        binder.bottomNavigationView.itemIconTintList = null

        requireActivity().onBackPressedDispatcher.addCallback(backCallback)

        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            backCallback.isEnabled = !isAtHomeTab(destination)
        }

        startBlur()
    }

    override fun applyInsets(rootView: View) {
        binder.bottomNavigationView.applyNavigationBarInsets()
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

    private fun startBlur() {
        val radiusInPx = 28.dp
        val offset = radiusInPx.toFloat() / 2f
        SweetBlur.ViewBackgroundBuilder()
            .blurColor(requireContext().getColor(R.color.blur_navigation_background))
            .captureExtraSpace(RectF(0f, offset, 0f, 0f))
            .cutSpace(RectF(0f, offset, 0f, offset))
            .radius(radiusInPx)
            .captureFrom(binder.bottomNavHost)
            .toTarget(binder.bottomNavigationView)
            .catchException { binder.bottomNavigationView.setBackgroundColorRes(R.color.solid_navigation_background) }
            .build()
//            .start()
    }
}
