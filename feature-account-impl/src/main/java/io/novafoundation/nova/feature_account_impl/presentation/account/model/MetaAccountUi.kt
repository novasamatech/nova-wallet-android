package io.novafoundation.nova.feature_account_impl.presentation.account.model

import android.graphics.drawable.Drawable

class MetaAccountUi(
    val id: Long,
    val name: String,
    val isSelected: Boolean,
    val totalBalance: String,
    val picture: Drawable,
)
