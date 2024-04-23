package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff

import android.content.Context
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.adapter.CloudBackupDiffAdapter
import kotlinx.android.synthetic.main.fragment_backup_diff.backupDiffApply
import kotlinx.android.synthetic.main.fragment_backup_diff.backupDiffCancel
import kotlinx.android.synthetic.main.fragment_backup_diff.backupDiffList
import kotlinx.android.synthetic.main.fragment_backup_diff.backupDiffSubtitle

class CloudBackupDiffBottomSheet(
    context: Context,
    payload: Payload,
    onApply: () -> Unit,
) : BaseBottomSheet(context) {

    class Payload(val diffList: List<Any>)

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        CloudBackupDiffAdapter()
    }

    init {
        setContentView(R.layout.fragment_backup_diff)
        backupDiffSubtitle.text = buildSubtitleText()
        backupDiffList.adapter = adapter

        backupDiffCancel.setOnClickListener { dismiss() }
        backupDiffApply.setOnClickListener {
            onApply()
            dismiss()
        }
        adapter.submitList(payload.diffList)
    }

    private fun buildSubtitleText(): CharSequence {
        return SpannableFormatter.format(
            context.getString(R.string.backup_diff_subtitle),
            context.getString(R.string.backup_diff_subtitle_highlighted).addColor(context.getColor(R.color.text_primary))
        )
    }
}
