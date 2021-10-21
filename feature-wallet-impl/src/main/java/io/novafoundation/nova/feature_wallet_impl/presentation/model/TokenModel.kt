package io.novafoundation.nova.feature_wallet_impl.presentation.model

import androidx.annotation.ColorRes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class TokenModel(
    val configuration: Chain.Asset,
    val dollarRate: String,
    val recentRateChange: String,
    @ColorRes val rateChangeColorRes: Int
)
