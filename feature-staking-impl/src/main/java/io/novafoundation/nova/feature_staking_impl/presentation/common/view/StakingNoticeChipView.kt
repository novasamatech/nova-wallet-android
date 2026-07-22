package io.novafoundation.nova.feature_staking_impl.presentation.common.view

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice

class StakingNoticeChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : AppCompatTextView(context, attrs, defStyle) {

    init {
        text = context.getString(R.string.staking_notice_chip_label)
        textSize = 11f
    }

    fun bind(notice: StakingNotice) {
        val (bgRes, fgRes) = when (notice.severity) {
            StakingNotice.Severity.CRITICAL ->
                R.color.notice_critical_bg to R.color.notice_critical_text
            StakingNotice.Severity.INFO ->
                R.color.notice_info_bg to R.color.notice_info_text
        }
        setBackgroundColor(ContextCompat.getColor(context, bgRes))
        setTextColor(ContextCompat.getColor(context, fgRes))
    }
}
