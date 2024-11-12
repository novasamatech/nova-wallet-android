package io.novafoundation.nova.feature_assets.presentation.send.common.fee

import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeDisplay

class TransferFeeDisplay(
    val originFee: FeeDisplay,
    val crossChainFee: FeeDisplay?
)
