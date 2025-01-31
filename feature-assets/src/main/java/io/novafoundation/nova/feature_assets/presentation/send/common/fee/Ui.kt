package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_assets.domain.send.model.TransferFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.mapDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.mapProgress
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

context(BaseFragment<V, *>)
fun <V : BaseViewModel> FeeLoaderMixinV2<TransferFee, TransferFeeDisplay>.setupFeeLoading(originFeeView: FeeView, crossChainFeeView: FeeView) {
    setupFeeLoading(
        setFeeStatus = {
            // We only apply `visibleInProgress` for cross-chain fee. This can be handled better with generic argument for Loading payload
            val originFee = it.mapDisplay(TransferFeeDisplay::originFee).mapProgress { true }
            val crossChainFee = it.mapDisplay(TransferFeeDisplay::crossChainFee)

            originFeeView.setFeeStatus(originFee)
            crossChainFeeView.setFeeStatus(crossChainFee)
        },
        setUserCanChangeFeeAsset = {
            originFeeView.setFeeEditable(it) {
                changePaymentCurrencyClicked()
            }
        }
    )
}
