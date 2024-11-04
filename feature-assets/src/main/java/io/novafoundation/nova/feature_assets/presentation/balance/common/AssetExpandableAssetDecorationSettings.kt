package io.novafoundation.nova.feature_assets.presentation.balance.common

import android.view.animation.AccelerateDecelerateInterpolator
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings

fun ExpandableAnimationSettings.Companion.createForAssets() = ExpandableAnimationSettings(400, AccelerateDecelerateInterpolator())
