package io.novafoundation.nova.feature_assets.presentation.buy.flow

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.setupBuyIntegration
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment

class AssetBuyFlowFragment : AssetFlowFragment<AssetBuyFlowViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .buyFlowComponent()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: AssetBuyFlowViewModel) {
        super.subscribe(viewModel)

        setupBuyIntegration(mixin = viewModel.buyMixin)
    }
}
