package io.novafoundation.nova.feature_staking_impl.presentation.staking.delegation.proxy.list.model

import android.graphics.drawable.Drawable

class StakingProxyGroupRvItem(val text: String)

class StakingProxyRvItem(
    val accountIcon: Drawable,
    val chainIconUrl: String?,
    val accountTitle: String,
    val accountAddress: String
)
