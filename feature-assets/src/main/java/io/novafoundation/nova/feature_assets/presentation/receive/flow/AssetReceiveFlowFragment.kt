package io.novafoundation.nova.feature_assets.presentation.receive.flow

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment

class AssetReceiveFlowFragment : AssetFlowFragment<AssetReceiveFlowViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitle(R.string.wallet_asset_receive)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .receiveFlowComponent()
            .create(this)
            .inject(this)
    }
}
