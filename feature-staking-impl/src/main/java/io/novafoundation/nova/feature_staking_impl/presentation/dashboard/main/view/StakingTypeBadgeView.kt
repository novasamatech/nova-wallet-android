package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel.StakingTypeModel

class StakingTypeBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(context) {

    init {
        setTextAppearance(R.style.TextAppearance_NovaFoundation_SemiBold_Caps2)
        setTextColorRes(R.color.chip_text)

        gravity = Gravity.CENTER_VERTICAL
        includeFontPadding = false

        background = getRoundedCornerDrawable(R.color.chips_background, cornerSizeDp = 6)
    }

    fun setModel(model: StakingTypeModel) {
        setDrawableStart(model.icon, widthInDp = 10, paddingInDp = 4, tint = R.color.icon_secondary)
        text = model.text
    }
}

fun StakingTypeBadgeView.setModelOrHide(maybeModel: StakingTypeModel?) = letOrHide(maybeModel, ::setModel)
