package io.novafoundation.nova.feature_wallet_api.presentation.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import io.novafoundation.nova.common.presentation.masking.setMaskableText
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getEnum
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.images.setIcon
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getIdleDrawable
import io.novafoundation.nova.feature_wallet_api.R
import io.novafoundation.nova.feature_wallet_api.databinding.ViewAssetSelectorBinding
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorModel

class AssetSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    enum class BackgroundStyle {
        BLURRED, BORDERED
    }

    private val binder = ViewAssetSelectorBinding.inflate(inflater(), this)

    init {
        attrs?.let {
            applyAttributes(it)
        }
    }

    private fun applyAttributes(attributes: AttributeSet) = context.useAttributes(attributes, R.styleable.AssetSelectorView) {
        val backgroundStyle: BackgroundStyle = it.getEnum(R.styleable.AssetSelectorView_backgroundStyle, BackgroundStyle.BORDERED)

        val actionIcon = it.getDrawable(R.styleable.AssetSelectorView_actionIcon)
        actionIcon?.let(::setActionIcon)

        setBackgroundStyle(backgroundStyle)
    }

    fun setActionIcon(drawable: Drawable) {
        binder.assetSelectorAction.setImageDrawable(drawable)
    }

    fun setBackgroundStyle(style: BackgroundStyle) = with(context) {
        val baseBackground = when (style) {
            BackgroundStyle.BLURRED -> getBlockDrawable()
            BackgroundStyle.BORDERED -> getIdleDrawable()
        }

        background = addRipple(baseBackground)
    }

    fun onClick(action: (View) -> Unit) {
        setOnClickListener(action)
    }

    fun setState(
        imageLoader: ImageLoader,
        assetSelectorModel: AssetSelectorModel
    ) {
        with(assetSelectorModel) {
            binder.assetSelectorBalance.setMaskableText(assetModel.assetBalance)
            binder.assetSelectorTokenName.text = title
            binder.assetSelectorIcon.setIcon(assetModel.icon, imageLoader)
        }
    }
}
