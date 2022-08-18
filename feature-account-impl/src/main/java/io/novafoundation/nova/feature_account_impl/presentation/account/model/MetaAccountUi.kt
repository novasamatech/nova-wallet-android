package io.novafoundation.nova.feature_account_impl.presentation.account.model

import android.graphics.drawable.Drawable


class MetaAccountUi(
    val id: Long,
    val title: String,
    val subtitle: String,
    val isSelected: Boolean,
    val isClickable: Boolean,
    val picture: Drawable,
    val subtitleIconRes: Int?
)
