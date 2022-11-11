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
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootComponent
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.blur.SweetBlur
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.updatePadding
import kotlinx.android.synthetic.main.fragment_main.bottomNavHost
import kotlinx.android.synthetic.main.fragment_main.bottomNavigationView

class MainFragment : BaseFragment<MainViewModel>() {

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

        bottomNavigationView.setupWithNavController(navController!!)

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            onNavDestinationSelected(item, navController!!)
        }
        bottomNavigationView.itemIconTintList = null

        requireActivity().onBackPressedDispatcher.addCallback(backCallback)

        navController!!.addOnDestinationChangedListener { _, destination, _ ->
            backCallback.isEnabled = !isAtHomeTab(destination)
        }
/*
        SweetBlur.getInstance().blurBackground(
            bottomNavigationView,
            bottomNavHost,
            extraSpace = RectF(0f, 65f, 0f, 0f),
            inset = RectF(30f, 0f, 30f, 65f),
            25,
            0.24f
        )*/

        val radiusInPx = 32.dp
        SweetBlur.getInstance().blurBackground(
            bottomNavigationView,
            bottomNavHost,
            extraSpace = RectF(0f, radiusInPx.toFloat()/2, 0f, 0f),
            inset = RectF(0f, 0f, 0f, radiusInPx.toFloat()/4),
            radiusInPx
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<RootComponent>(this, RootApi::class.java)
            .mainFragmentComponentFactory()
            .create(requireActivity())
            .inject(this)
    }

    override fun subscribe(viewModel: MainViewModel) {
        viewModel.stakingAvailableLiveData.observe {
            bottomNavigationView.menu.findItem(R.id.stakingFragment).isVisible = it
        }
    }

    private fun isAtHomeTab(destination: NavDestination) =
        destination.id == navController!!.graph.startDestination
}
