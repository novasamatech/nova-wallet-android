package io.novafoundation.nova.feature_assets.presentation.send.flow.network

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadHolder
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload

class NetworkSendFlowFragment :
    NetworkFlowFragment<NetworkSendFlowViewModel>(),
    FragmentPayloadHolder<NetworkFlowPayload> {

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .networkSendFlowComponent()
            .create(this, payload)
            .inject(this)
    }
}
