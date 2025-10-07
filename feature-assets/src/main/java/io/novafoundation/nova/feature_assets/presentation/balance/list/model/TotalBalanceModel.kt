package io.novafoundation.nova.feature_assets.presentation.balance.list.model

import io.novafoundation.nova.common.presentation.masking.MaskableModel

class TotalBalanceModel(
    val isBreakdownAvailable: Boolean,
    val totalBalanceFiat: MaskableModel<CharSequence>,
    val lockedBalanceFiat: MaskableModel<CharSequence>,
    val enableSwap: Boolean
)
