package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardContainer
import kotlinx.android.synthetic.main.fragment_nova_card.novaCardWebView

class NovaCardFragment : BaseFragment<NovaCardViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_nova_card, container, false)

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
        novaCardContainer.applyStatusBarInsets()
    }

    override fun subscribe(viewModel: NovaCardViewModel) {
        viewModel.novaCardWebViewControllerFlow.observeFirst { controller ->
            controller.setup(novaCardWebView)
        }
    }
}
