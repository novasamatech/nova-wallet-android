package io.novafoundation.nova.feature_assets.presentation.swap.network

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadCreator
import io.novafoundation.nova.common.utils.PayloadCreator
import io.novafoundation.nova.common.utils.payload
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment

class NetworkSwapFlowFragment :
    NetworkFlowFragment<NetworkSwapFlowViewModel>() {

    companion object : PayloadCreator<NetworkSwapFlowPayload> by FragmentPayloadCreator()

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .networkSwapFlowComponent()
            .create(this, payload())
            .inject(this)
    }
}
