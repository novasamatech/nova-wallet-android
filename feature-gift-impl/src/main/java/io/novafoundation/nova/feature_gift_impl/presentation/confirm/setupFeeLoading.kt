package io.novafoundation.nova.feature_gift_impl.presentation.confirm

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.feature_gift_impl.domain.models.GiftFee
import io.novafoundation.nova.feature_gift_impl.presentation.amount.fee.GiftFeeDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.mapDisplay
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.setupFeeLoading
import io.novafoundation.nova.feature_wallet_api.presentation.view.FeeView

context(BaseFragment<V, *>)
fun <V : BaseViewModel> FeeLoaderMixinV2<GiftFee, GiftFeeDisplay>.setupGiftFeeLoading(networkFee: FeeView, claimFee: FeeView) {
    setupFeeLoading(
        setFeeStatus = {
            val originFee = it.mapDisplay(GiftFeeDisplay::networkFee)
            val crossChainFee = it.mapDisplay(GiftFeeDisplay::claimGiftFee)

            networkFee.setFeeStatus(originFee)
            claimFee.setFeeStatus(crossChainFee)
        },
        setUserCanChangeFeeAsset = {
            networkFee.setFeeEditable(it) {
                changePaymentCurrencyClicked()
            }
        }
    )
}
