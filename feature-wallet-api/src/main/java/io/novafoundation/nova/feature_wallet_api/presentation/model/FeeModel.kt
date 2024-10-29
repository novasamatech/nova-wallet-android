package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.feature_account_api.data.model.FeeBase

class FeeModel<F : FeeBase>(
    val fee: F,
    val display: AmountModel,
)
