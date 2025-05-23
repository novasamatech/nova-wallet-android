package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.settings.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.getRippleMask
import io.novafoundation.nova.common.utils.getRoundedCornerDrawable
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.withRippleMask
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.ovalDrawable
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.ViewCloudBackupStateBinding

class CloudBackupStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val binder = ViewCloudBackupStateBinding.inflate(inflater(), this, true)

    private val stateImage: ImageView
        get() = binder.backupStateImg

    private val progress: ProgressBar
        get() = binder.backupStateProgress

    private val title: TextView
        get() = binder.backupStateTitle

    private val subtitle: TextView
        get() = binder.backupStateSubtitle

    private val more: View
        get() = binder.backupStateMore

    private val divider: View
        get() = binder.backupStateDivider

    private val problemButton: TextView
        get() = binder.backupStateProblemBtn

    init {
        background = getRoundedCornerDrawable(fillColorRes = R.color.block_background).withRippleMask()
        problemButton.background = context.getRoundedCornerDrawable(fillColorRes = null).withRippleMask(getRippleMask(cornerSizeDp = 8))
    }

    fun setStateImage(resId: Int?, stateImageTiniRes: Int?, backgroundColorRes: Int?) {
        resId?.let { stateImage.setImageResource(it) } ?: stateImage.setImageDrawable(null)
        stateImage.setImageTint(stateImageTiniRes?.let { context.getColor(it) })
        stateImage.background = backgroundColorRes?.let { ovalDrawable(context.getColor(it)) }
    }

    fun setProgressVisibility(visible: Boolean) {
        progress.isVisible = visible
        stateImage.isGone = visible
    }

    fun setTitle(titleText: String) {
        title.text = titleText
    }

    fun setSubtitle(text: String?) {
        subtitle.setTextOrHide(text)
    }

    fun setProblemText(text: String?) {
        divider.isVisible = text != null
        problemButton.isVisible = text != null
        problemButton.text = text
    }

    fun setProblemClickListener(listener: OnClickListener?) {
        problemButton.setOnClickListener(listener)
    }

    fun showMoreButton(show: Boolean) {
        more.isVisible = show
    }
}
