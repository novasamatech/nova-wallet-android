package io.novafoundation.nova.splash.presentation

import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.splash.databinding.FragmentSplashBinding
import io.novafoundation.nova.splash.di.SplashFeatureApi
import io.novafoundation.nova.splash.di.SplashFeatureComponent
import javax.inject.Inject

class SplashFragment : BaseFragment<SplashViewModel, FragmentSplashBinding>() {

    override fun createBinding() = FragmentSplashBinding.inflate(layoutInflater)

    @Inject lateinit var splashViewModel: SplashViewModel

    override fun initViews() {
    }

    override fun inject() {
        FeatureUtils.getFeature<SplashFeatureComponent>(this, SplashFeatureApi::class.java)
            .splashComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.openInitialDestination()
    }

    override fun onDestroy() {
        super.onDestroy()

        (activity as? SplashBackgroundHolder)?.removeSplashBackground()
    }

    override fun subscribe(viewModel: SplashViewModel) {}
}
