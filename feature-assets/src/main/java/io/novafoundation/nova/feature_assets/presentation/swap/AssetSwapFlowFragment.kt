package io.novafoundation.nova.feature_assets.presentation.swap

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment
import kotlinx.android.synthetic.main.fragment_asset_flow_search.assetFlowPlaceholder

class AssetSwapFlowFragment : AssetFlowFragment<AssetSwapFlowViewModel>() {

    companion object {

        private const val KEY_PAYLOAD = "AssetSwapFlowFragment.payload"

        fun getBundle(payload: SwapFlowPayload): Bundle {
            return Bundle().apply {
                putParcelable(KEY_PAYLOAD, payload)
            }
        }
    }

    override fun initViews() {
        super.initViews()
        setTitle(viewModel.getTitleRes())
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .swapFlowComponent()
            .create(this, argument(KEY_PAYLOAD))
            .inject(this)
    }
}
