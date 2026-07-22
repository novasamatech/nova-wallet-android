package io.novafoundation.nova.feature_staking_impl.presentation.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.notices.model.StakingNotice

class StakingNoticeStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    private val textView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_staking_notice_strip, this)
        textView = findViewById(R.id.stakingNoticeStripText)
    }

    fun bind(notice: StakingNotice) {
        textView.text = notice.shortText
        when (notice.severity) {
            StakingNotice.Severity.CRITICAL -> {
                setBackgroundColor(ContextCompat.getColor(context, R.color.notice_critical_bg))
                textView.setTextColor(ContextCompat.getColor(context, R.color.notice_critical_text))
            }
            StakingNotice.Severity.INFO -> {
                setBackgroundColor(ContextCompat.getColor(context, R.color.notice_info_bg))
                textView.setTextColor(ContextCompat.getColor(context, R.color.notice_info_text))
            }
        }
    }
}
