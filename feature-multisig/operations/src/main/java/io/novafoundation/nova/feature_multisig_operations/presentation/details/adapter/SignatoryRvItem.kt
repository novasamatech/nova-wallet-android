package io.novafoundation.nova.feature_multisig_operations.presentation.details.adapter

import android.graphics.drawable.Drawable

data class SignatoryRvItem(
    val address: String,
    val icon: Drawable,
    val title: String,
    val subtitle: CharSequence?,
    val isApproved: Boolean,
)
