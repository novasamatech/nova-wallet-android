package io.novafoundation.nova.feature_assets.presentation.gifts.assets

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.asset.AssetFlowFragment

class AssetGiftsFlowFragment : AssetFlowFragment<AssetGiftsFlowViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitle(R.string.gifts_assets_flow_title)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .giftsFlowComponent()
            .create(this)
            .inject(this)
    }
}
