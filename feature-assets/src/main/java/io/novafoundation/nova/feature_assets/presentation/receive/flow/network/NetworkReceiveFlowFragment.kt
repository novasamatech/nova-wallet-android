package io.novafoundation.nova.feature_assets.presentation.receive.flow.network

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadHolder
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload

class NetworkReceiveFlowFragment :
    NetworkFlowFragment<NetworkReceiveFlowViewModel>(),
    FragmentPayloadHolder<NetworkFlowPayload> {

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .networkReceiveFlowComponent()
            .create(this, payload)
            .inject(this)
    }
}
