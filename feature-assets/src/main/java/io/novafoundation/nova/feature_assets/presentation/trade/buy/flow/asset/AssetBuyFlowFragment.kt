package io.novafoundation.nova.feature_assets.presentation.trade.buy.flow.asset

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowFragment

class AssetBuyFlowFragment : AssetFlowFragment<AssetBuyFlowViewModel>() {

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
}
