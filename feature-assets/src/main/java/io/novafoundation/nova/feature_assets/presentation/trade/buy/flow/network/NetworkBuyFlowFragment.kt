package io.novafoundation.nova.feature_assets.presentation.trade.buy.flow.network

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment

class NetworkBuyFlowFragment : NetworkFlowFragment<NetworkBuyFlowViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .networkBuyFlowComponent()
            .create(this, payload())
            .inject(this)
    }
}
