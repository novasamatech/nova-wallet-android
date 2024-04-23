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
import io.novafoundation.nova.common.utils.setImageTint
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.withRippleMask
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.common.view.shape.ovalDrawable
import io.novafoundation.nova.feature_settings_impl.R
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateDivider
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateImg
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateMore
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateProblemBtn
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateProgress
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateSubtitle
import kotlinx.android.synthetic.main.view_cloud_backup_state.view.backupStateTitle

class CloudBackupStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val stateImage: ImageView
        get() = backupStateImg

    private val progress: ProgressBar
        get() = backupStateProgress

    private val title: TextView
        get() = backupStateTitle

    private val subtitle: TextView
        get() = backupStateSubtitle

    private val more: View
        get() = backupStateMore

    private val divider: View
        get() = backupStateDivider

    private val problemButton: TextView
        get() = backupStateProblemBtn

    init {
        View.inflate(context, R.layout.view_cloud_backup_state, this)

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
