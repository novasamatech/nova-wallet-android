package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.flow.Flow

class SwapConfirmationViewModel(
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val fromAsset: Flow<SwapAssetView.Model> = flowOf {
        SwapAssetView.Model(
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/icons/tokens/white/USDT.svg",
            AmountModel("50 USDT", "~\$49.9"),
            Icon.FromLink("https://raw.githubusercontent.com/novasamatech/nova-utils/master/icons/chains/gradient/Kusama_Asset_Hub.svg"),
            "Select a token"
        )
    }
    val toAsset: Flow<SwapAssetView.Model> = flowOf {
        SwapAssetView.Model(
            "https://raw.githubusercontent.com/novasamatech/nova-utils/master/icons/chains/white/Polkadot.svg",
            AmountModel("10 DOT", "~\$44.6"),
            Icon.FromLink("https://raw.githubusercontent.com/novasamatech/nova-utils/master/icons/chains/gradient/Polkadot.svg"),
            "Select a token"
        )
    }

    val rateDetails: Flow<String> = flowOf { "1 USDT ≈ 0.21 DOT" }
    val priceDifference: Flow<String> = flowOf { "−0.14%" }
    val slippage: Flow<String> = flowOf { "0.5%" }
    val networkFee: Flow<AmountModel> = flowOf { AmountModel("0.01524 DOT", "\$0.07") }

    val wallet: Flow<WalletModel> = flowOf { WalletModel(0, "Anthony", null) }
    val account: Flow<AddressModel> = flowOf {
        AddressModel("J6b7XsdA42gafKso3gSsd9KsapTtASfr2z4rPt", resourceManager.getDrawable(R.drawable.ic_nova_logo))
    }

    fun rateClicked() {
        TODO("Not yet implemented")
    }

    fun priceDifferenceClicked() {
        TODO("Not yet implemented")
    }

    fun slippageClicked() {
        TODO("Not yet implemented")
    }

    fun networkFeeClicked() {
        TODO("Not yet implemented")
    }

    fun accountClicked() {
        TODO("Not yet implemented")
    }

    fun confirmButtonClicked() {
        TODO("Not yet implemented")
    }
}
