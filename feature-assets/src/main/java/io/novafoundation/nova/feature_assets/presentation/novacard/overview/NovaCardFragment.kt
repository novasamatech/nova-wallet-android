package io.novafoundation.nova.feature_assets.presentation.novacard.overview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.novacard.topup.TopUpCardViewModel

class NovaCardFragment : BaseFragment<TopUpCardViewModel>() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = layoutInflater.inflate(R.layout.fragment_nova_card, container, false)

    override fun initViews() {
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(
            requireContext(),
            AssetsFeatureApi::class.java
        )
            .novaCardComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: TopUpCardViewModel) {

    }
}
