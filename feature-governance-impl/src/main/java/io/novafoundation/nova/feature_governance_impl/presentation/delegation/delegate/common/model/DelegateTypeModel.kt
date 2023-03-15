package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.view.setPadding
import coil.ImageLoader
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.view.NovaChipView

class DelegateTypeModel(
    val text: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val textColorRes: Int,
    @ColorRes val iconColorRes: Int,
    @ColorRes val backgroundColorRes: Int,
)

class DelegateIcon(val shape: IconShape, val icon: Icon) {

    enum class IconShape {
        ROUND, SQUARE, NONE
    }
}

fun NovaChipView.setDelegateTypeModel(model: DelegateTypeModel?) {
    setVisible(model != null)
    if (model == null) return

    setText(model.text)
    setIcon(model.iconRes)
    setStyle(model.backgroundColorRes, model.textColorRes, model.iconColorRes)
}

fun NovaChipView.setDelegateTypeModelIcon(model: DelegateTypeModel?) {
    setVisible(model != null)
    if (model == null) return

    setIcon(model.iconRes)
    setStyle(model.backgroundColorRes, model.textColorRes, model.iconColorRes)
}

fun ImageView.setDelegateIcon(
    icon: DelegateIcon,
    imageLoader: ImageLoader,
    squareCornerRadiusDp: Int
) {
    val strokeWidthDp = 0.5f
    val borderPadding = strokeWidthDp.dp(context)
        .coerceAtLeast(1)
    setPadding(borderPadding)

    if (icon.shape == DelegateIcon.IconShape.SQUARE) {
        background = context.getRoundedCornerDrawable(
            null,
            R.color.container_border,
            cornerSizeInDp = squareCornerRadiusDp,
            strokeSizeInDp = strokeWidthDp
        )
    } else {
        setBackgroundResource(R.drawable.bg_induvidual_circle_border)
    }

    setIcon(icon.icon, imageLoader) {
        icon.shape.coilTransformation(squareCornerRadiusDp.dpF(context))?.let {
            transformations(it)
        }
    }
}

private fun DelegateIcon.IconShape.coilTransformation(cornerRadiusPx: Float): Transformation? {
    return when (this) {
        DelegateIcon.IconShape.ROUND -> CircleCropTransformation()
        DelegateIcon.IconShape.SQUARE -> RoundedCornersTransformation(cornerRadiusPx)
        DelegateIcon.IconShape.NONE -> null
    }
}
