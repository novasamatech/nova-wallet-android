package io.novafoundation.nova.feature_assets.presentation.balance.detail

import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.feature_assets.presentation.model.TokenModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class AssetDetailsModel(
    val token: TokenModel,
    val assetIcon: Icon,
    val total: AmountModel,
    val transferable: AmountModel,
    val locked: AmountModel
)
