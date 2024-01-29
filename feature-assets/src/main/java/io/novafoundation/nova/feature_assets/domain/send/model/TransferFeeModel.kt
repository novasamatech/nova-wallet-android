package io.novafoundation.nova.feature_assets.domain.send.model

import io.novafoundation.nova.feature_account_api.data.model.Fee

class TransferFeeModel(
    val originFee: Fee,
    val crossChainFee: Fee?
)
