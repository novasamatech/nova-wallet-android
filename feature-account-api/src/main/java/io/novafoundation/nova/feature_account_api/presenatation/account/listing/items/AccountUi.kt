package io.novafoundation.nova.feature_account_api.presenatation.account.listing.items

import android.graphics.drawable.Drawable

class AccountUi(
    val id: Long,
    val title: CharSequence,
    val subtitle: CharSequence,
    val isSelected: Boolean,
    val isClickable: Boolean,
    val picture: Drawable,
    val chainIconUrl: String?,
    val updateIndicator: Boolean,
    val subtitleIconRes: Int?,
    val chainIconOpacity: Float = 1f
)
