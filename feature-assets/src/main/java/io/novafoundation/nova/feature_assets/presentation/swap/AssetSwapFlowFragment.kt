package io.novafoundation.nova.feature_assets.presentation.swap

import android.os.Bundle
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment

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

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        showMessage("Not implemented yet")
    }
}
