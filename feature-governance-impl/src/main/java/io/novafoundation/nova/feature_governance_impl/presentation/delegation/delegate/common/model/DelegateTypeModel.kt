package io.novafoundation.nova.feature_governance_impl.presentation.delegation.delegate.common.model

import android.content.Context
import android.widget.ImageView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import coil.ImageLoader
import coil.transform.CircleCropTransformation
import coil.transform.RoundedCornersTransformation
import coil.transform.Transformation
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.DEFAULT_CORNER_RADIUS
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

fun ImageView.setDelegateIcon(icon: DelegateIcon, imageLoader: ImageLoader) {
    setIcon(icon.icon, imageLoader) {
        icon.shape.coilTransformation(context)?.let {
            transformations(it)
        }
    }
}

private fun DelegateIcon.IconShape.coilTransformation(context: Context): Transformation? {
    return when (this) {
        DelegateIcon.IconShape.ROUND -> CircleCropTransformation()
        DelegateIcon.IconShape.SQUARE -> RoundedCornersTransformation(DEFAULT_CORNER_RADIUS.dpF(context))
        DelegateIcon.IconShape.NONE -> null
    }
}
