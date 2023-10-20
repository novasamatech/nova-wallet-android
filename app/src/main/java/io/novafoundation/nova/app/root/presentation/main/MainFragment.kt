package io.novafoundation.nova.app.root.presentation.main

import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.app.root.navigation.staking.StakingDashboardNavigator
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.blur.SweetBlur
import io.novafoundation.nova.common.utils.setBackgroundColorRes
import io.novafoundation.nova.common.utils.updatePadding
import kotlinx.android.synthetic.main.fragment_main.bottomNavHost
import kotlinx.android.synthetic.main.fragment_main.bottomNavigationView
import javax.inject.Inject

class MainFragment : BaseFragment<MainViewModel>() {

    @Inject
    lateinit var stakingDashboardNavigator: StakingDashboardNavigator

    private var navController: NavController? = null

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            isEnabled = navController!!.navigateUp()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        backCallback.isEnabled = false
        stakingDashboardNavigator.clearStakingTabNavController()
    }

    override fun initViews() {
        bottomNavigationView.setOnApplyWindowInsetsListener { _, insets ->
            // overwrite BottomNavigation behavior and ignore insets
            insets
        }

        bottomNavHost.setOnApplyWindowInsetsListener { v, insets ->
            val systemWindowInsetBottom = insets.systemWindowInsetBottom

            // post to prevent bottomNavigationView.height being 0 if callback is called before view has been measured
            v.post {
                val padding = (systemWindowInsetBottom - bottomNavigationView.height).coerceAtLeast(0)
                v.updatePadding(bottom = padding)
            }

            insets
        }

        val nestedNavHostFragment =
            childFragmentManager.findFragmentById(R.id.bottomNavHost) as NavHostFragment

        navController = nestedNavHostFragment.navController
        stakingDashboardNavigator.setStakingTabNavController(navController!!)

        bottomNavigationView.setupWithNavController(navController!!)
        bottomNavigationView.itemIconTintList = null

        requireActivity().onBackPressedDispatcher.addCallback(backCallback)

        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            backCallback.isEnabled = !isAtHomeTab(destination)
        }

        startBlur()
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
            .captureFrom(bottomNavHost)
            .toTarget(bottomNavigationView)
            .catchException { bottomNavigationView.setBackgroundColorRes(R.color.solid_navigation_background) }
            .build()
//            .start()
    }
}
