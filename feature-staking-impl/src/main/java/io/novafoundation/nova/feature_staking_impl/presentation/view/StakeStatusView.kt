package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatTextView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_staking_impl.R

data class StakeStatusModel(
    @DrawableRes val indicatorRes: Int,
    val text: String,
    @ColorRes val textColorRes: Int
)

class StakeStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr),
    WithContextExtensions by WithContextExtensions(context) {

    init {
        background = getRoundedCornerDrawable(fillColorRes = R.color.chips_background)
    }

    fun setModel(stakeStatusModel: StakeStatusModel) {
        setStatusIndicator(stakeStatusModel.indicatorRes)
        setDrawableStart(stakeStatusModel.indicatorRes, widthInDp = 14, paddingInDp = 5)

        text = stakeStatusModel.text
        setTextColorRes(stakeStatusModel.textColorRes)
    }

    fun setStatusIndicator(@DrawableRes indicatorRes: Int) {
        setDrawableStart(indicatorRes, widthInDp = 14, paddingInDp = 5)
    }
}
