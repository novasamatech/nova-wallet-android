package io.novafoundation.nova.feature_assets.presentation.balance.list.model

class TotalBalanceModel(
    val isLocksAvailable: Boolean,
    val totalBalanceFiat: String,
    val lockedBalanceFiat: String
)
