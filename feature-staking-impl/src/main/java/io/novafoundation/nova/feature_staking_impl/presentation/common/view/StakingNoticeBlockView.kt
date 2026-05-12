package io.novafoundation.nova.feature_staking_impl.presentation.common.view

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice

class StakingNoticeBlockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val titleView: TextView
    private val bodyView: TextView

    init {
        orientation = VERTICAL
        val paddingHorizontal = dp(16)
        val paddingVertical = dp(12)
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        LayoutInflater.from(context).inflate(R.layout.view_staking_notice_block, this)
        titleView = findViewById(R.id.stakingNoticeBlockTitle)
        bodyView = findViewById(R.id.stakingNoticeBlockBody)
    }

    fun bind(notice: StakingNotice) {
        titleView.text = notice.shortText
        bodyView.text = notice.longText
        val (bgRes, fgRes) = when (notice.severity) {
            StakingNotice.Severity.CRITICAL ->
                R.color.notice_critical_bg to R.color.notice_critical_text
            StakingNotice.Severity.INFO ->
                R.color.notice_info_bg to R.color.notice_info_text
        }
        setBackgroundColor(ContextCompat.getColor(context, bgRes))
        titleView.setTextColor(ContextCompat.getColor(context, fgRes))
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
