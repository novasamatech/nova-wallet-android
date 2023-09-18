package io.novafoundation.nova.feature_assets.presentation.send.flow

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowPlaceholder

class AssetSendFlowFragment : AssetFlowFragment<AssetSendFlowViewModel>() {

    override fun initViews() {
        super.initViews()
        setTitle(R.string.wallet_asset_send)

        assetFlowPlaceholder.setButtonClickListener {
            viewModel.openBuyFlow()
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .sendFlowComponent()
            .create(this)
            .inject(this)
    }
}
