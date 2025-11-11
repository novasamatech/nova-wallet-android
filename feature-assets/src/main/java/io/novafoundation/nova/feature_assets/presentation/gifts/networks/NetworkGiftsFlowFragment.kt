package io.novafoundation.nova.feature_assets.presentation.gifts.networks

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment

class NetworkGiftsFlowFragment :
    NetworkFlowFragment<NetworkGiftsFlowViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .networkGiftsFlowComponent()
            .create(this, payload())
            .inject(this)
    }
}
