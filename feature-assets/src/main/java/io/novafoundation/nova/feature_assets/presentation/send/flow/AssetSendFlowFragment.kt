package io.novafoundation.nova.feature_assets.presentation.send.flow

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment

class AssetSendFlowFragment : AssetFlowFragment<AssetSendFlowViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .sendFlowComponent()
            .create(this)
            .inject(this)
    }
}
