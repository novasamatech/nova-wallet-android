package io.novafoundation.nova.feature_assets.presentation.trade.sell.flow.asset

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowFragment

class AssetSellFlowFragment : AssetFlowFragment<AssetSellFlowViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitle(R.string.wallet_asset_sell)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .sellFlowComponent()
            .create(this)
            .inject(this)
    }
}
