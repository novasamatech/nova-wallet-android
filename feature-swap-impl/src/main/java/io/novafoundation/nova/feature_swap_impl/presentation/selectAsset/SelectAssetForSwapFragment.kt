package io.novafoundation.nova.feature_swap_impl.presentation.selectAsset

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.assetActions.buy.setupBuyIntegration
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment

class SelectAssetForSwapFragment : AssetFlowFragment<AssetBuyFlowViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitle(R.string.wallet_asset_buy)
    }

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
