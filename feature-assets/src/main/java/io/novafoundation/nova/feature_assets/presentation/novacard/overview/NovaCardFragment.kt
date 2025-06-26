package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_assets.databinding.FragmentNovaCardBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent

class NovaCardFragment : BaseFragment<NovaCardViewModel, FragmentNovaCardBinding>() {

    override fun createBinding() = FragmentNovaCardBinding.inflate(layoutInflater)

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .novaCardComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun initViews() {
        binder.novaCardToolbar.applyStatusBarInsets()
        binder.novaCardToolbar.setHomeButtonListener { viewModel.backClicked() }
    }

    override fun subscribe(viewModel: NovaCardViewModel) {
        viewModel.novaCardWebViewControllerFlow.observeFirst { controller ->
            controller.setup(binder.novaCardWebView)
        }
    }
}
