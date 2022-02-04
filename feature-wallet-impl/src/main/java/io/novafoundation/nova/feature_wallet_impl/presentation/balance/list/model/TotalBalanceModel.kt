package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model

class TotalBalanceModel(
    val shouldShowPlaceholder: Boolean,
    val totalBalanceFiat: String,
    val lockedBalanceFiat: String
)
