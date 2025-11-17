package io.novafoundation.nova.feature_gift_impl.presentation.gifts

import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.images.Icon

data class GiftRVItem(
    val id: Long,
    val isClaimed: Boolean,
    val amount: CharSequence,
    val assetIcon: Icon,
    val subtitle: String,
    @DrawableRes val imageRes: Int
)
