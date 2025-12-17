package io.novafoundation.nova.feature_gift_impl.presentation.amount.fee

import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay

class GiftFeeDisplay(
    val networkFee: FeeDisplay,
    val claimGiftFee: FeeDisplay,
)
