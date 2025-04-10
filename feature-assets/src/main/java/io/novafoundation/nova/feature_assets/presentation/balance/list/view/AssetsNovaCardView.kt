package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER_VERTICAL
import android.widget.LinearLayout
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_assets.databinding.ViewAssetNovaCardBinding

class AssetsNovaCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    private val binder = ViewAssetNovaCardBinding.inflate(inflater(), this)

    override val providedContext: Context = context

    init {
        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL

        background = addRipple(getRoundedCornerDrawable(R.color.block_background))
    }

    fun setText(text: CharSequence) {
        binder.assetNovaCardText.text = text
    }
}
