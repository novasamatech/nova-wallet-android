package io.novafoundation.nova.feature_assets.presentation.buy.flow.network

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.FragmentPayloadHolder
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowFragment
import io.novafoundation.nova.feature_assets.presentation.flow.network.NetworkFlowPayload
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import javax.inject.Inject

class NetworkBuyFlowFragment :
    NetworkFlowFragment<NetworkBuyFlowViewModel>(),
    FragmentPayloadHolder<NetworkFlowPayload> {

    @Inject
    lateinit var buyMixin: BuyMixinUi

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .networkBuyFlowComponent()
            .create(this, payload)
            .inject(this)
    }

    override fun subscribe(viewModel: NetworkBuyFlowViewModel) {
        super.subscribe(viewModel)

        buyMixin.setupBuyIntegration(this, viewModel.buyMixin)
    }
}
