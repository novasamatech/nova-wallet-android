package io.novafoundation.nova.feature_assets.presentation.balance.list.model

class TotalBalanceModel(
    val shouldShowPlaceholder: Boolean,
    val shouldShowLockedBalance: Boolean,
    val totalBalanceFiat: String,
    val lockedBalanceFiat: String
)
