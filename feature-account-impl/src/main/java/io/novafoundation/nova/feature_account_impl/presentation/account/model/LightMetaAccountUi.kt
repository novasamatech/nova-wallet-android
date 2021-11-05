package io.novafoundation.nova.feature_account_impl.presentation.account.model

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.utils.IgnoredOnEquals

data class LightMetaAccountUi(
    val id: Long,
    val name: String,
    val isSelected: Boolean,
    val picture: IgnoredOnEquals<Drawable>,
)
