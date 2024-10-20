package io.novafoundation.nova.feature_assets.presentation.balance.list.model.items

import androidx.annotation.ColorRes
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

data class TokenGroupUi(
    val tokenIcon: String?,
    val rate: String,
    val recentRateChange: String,
    @ColorRes val rateChangeColorRes: Int,
    val tokenSymbol: String,
    val balance: AmountModel
) : AssetGroupRvItem {
    override val itemId: String = tokenSymbol
}
