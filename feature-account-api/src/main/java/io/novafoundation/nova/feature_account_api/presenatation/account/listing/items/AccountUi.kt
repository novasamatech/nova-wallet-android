package io.novafoundation.nova.feature_account_api.presenatation.account.listing.items

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.utils.images.Icon

class AccountUi(
    val id: Long,
    val title: CharSequence,
    val subtitle: CharSequence?,
    val isSelected: Boolean,
    val isEditable: Boolean,
    val isClickable: Boolean,
    val picture: Drawable,
    val chainIcon: Icon?,
    val updateIndicator: Boolean,
    val subtitleIconRes: Int?,
    val chainIconOpacity: Float = 1f
)
