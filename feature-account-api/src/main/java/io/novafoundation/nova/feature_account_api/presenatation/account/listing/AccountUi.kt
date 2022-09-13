package io.novafoundation.nova.feature_account_api.presenatation.account.listing

import android.graphics.drawable.Drawable

class AccountUi(
    val id: Long,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
    val isClickable: Boolean,
    val picture: Drawable,
    val subtitleIconRes: Int?
)
