package io.novafoundation.nova.feature_staking_impl.presentation.common.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
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
        val paddingHorizontal = dp(14)
        val paddingVertical = dp(14)
        setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
        LayoutInflater.from(context).inflate(R.layout.view_staking_notice_block, this)
        titleView = findViewById(R.id.stakingNoticeBlockTitle)
        bodyView = findViewById(R.id.stakingNoticeBlockBody)
    }

    fun bind(notice: StakingNotice) {
        titleView.text = notice.shortText
        bodyView.text = notice.longText
        val (bgRes, fgRes, borderAlpha) = when (notice.severity) {
            StakingNotice.Severity.CRITICAL ->
                Triple(R.color.notice_critical_bg, R.color.notice_critical_text, 0.30f)
            StakingNotice.Severity.INFO ->
                Triple(R.color.notice_info_bg, R.color.notice_info_text, 0.25f)
        }
        val bgColor = ContextCompat.getColor(context, bgRes)
        val fgColor = ContextCompat.getColor(context, fgRes)
        val borderColor = ColorUtils.setAlphaComponent(fgColor, (borderAlpha * 255).toInt())

        background = GradientDrawable().apply {
            cornerRadius = dp(12).toFloat()
            setColor(bgColor)
            setStroke(dp(1), borderColor)
        }
        titleView.setTextColor(fgColor)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
