package io.novafoundation.nova.feature_assets.domain.send.model

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.domain.model.OriginFee

class TransferFeeModel(
    val originFee: OriginFee,
    val crossChainFee: FeeBase?
)
