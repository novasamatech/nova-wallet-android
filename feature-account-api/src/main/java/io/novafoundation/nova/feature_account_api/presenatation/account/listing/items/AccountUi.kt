package io.novafoundation.nova.feature_account_api.presenatation.account.listing.items

import android.graphics.drawable.Drawable

class AccountUi(
    val id: Long,
    val title: String,
    val subtitle: CharSequence,
    val isSelected: Boolean,
    val isClickable: Boolean,
    val picture: Drawable,
    val chainIconUrl: String?,
    val enabled: Boolean,
    val updateIndicator: Boolean,
    val subtitleIconRes: Int?
)
