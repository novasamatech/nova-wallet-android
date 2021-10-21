package io.novafoundation.nova.feature_wallet_impl.presentation.balance.detail

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_impl.presentation.model.TokenModel

class AssetDetailsModel(
    val token: TokenModel,
    val total: AmountModel,
    val transferable: AmountModel,
    val locked: AmountModel
)
