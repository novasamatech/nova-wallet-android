package io.novafoundation.nova.feature_assets.presentation.model

import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class AssetModel(
    val token: TokenModel,
    val amount: MaskableModel<AmountModel>
)
