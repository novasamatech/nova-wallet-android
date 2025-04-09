package io.novafoundation.nova.feature_assets.presentation.balance.list.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity.CENTER_VERTICAL
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import kotlinx.android.synthetic.main.view_asset_nova_card.view.assetNovaCardText

class AssetsNovaCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_asset_nova_card, this)

        orientation = HORIZONTAL
        gravity = CENTER_VERTICAL

        background = addRipple(getRoundedCornerDrawable(R.color.block_background))
    }

    fun setText(text: CharSequence) {
        assetNovaCardText.text = text
    }
}
