package io.novafoundation.nova.feature_assets.presentation.buy.flow

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowFragment
import io.novafoundation.nova.feature_buy_api.presentation.mixin.BuyMixinUi
import javax.inject.Inject

class AssetBuyFlowFragment : AssetFlowFragment<AssetBuyFlowViewModel>() {

    @Inject
    lateinit var buyMixin: BuyMixinUi

    override fun initViews() {
        super.initViews()
        setTitle(R.string.wallet_asset_buy)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .buyFlowComponent()
            .create(this)
            .inject(this)
    }

    override fun tokenGroupClicked(tokenGroup: TokenGroupUi) {
        showMessage("Not implemented yet")
    }

    override fun subscribe(viewModel: AssetBuyFlowViewModel) {
        super.subscribe(viewModel)

        buyMixin.setupBuyIntegration(this, viewModel.buyMixin)
    }
}
