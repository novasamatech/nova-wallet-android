package io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff

import android.content.Context
import android.view.LayoutInflater
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff.CloudBackupDiff
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.databinding.FragmentBackupDiffBinding
import io.novafoundation.nova.feature_settings_impl.presentation.cloudBackup.backupDiff.adapter.CloudBackupDiffAdapter

class CloudBackupDiffBottomSheet(
    context: Context,
    private val payload: Payload,
    onApply: (CloudBackupDiff, CloudBackup) -> Unit,
) : BaseBottomSheet<FragmentBackupDiffBinding>(context) {

    override val binder: FragmentBackupDiffBinding = FragmentBackupDiffBinding.inflate(LayoutInflater.from(context))

    class Payload(val diffList: List<Any>, val cloudBackupDiff: CloudBackupDiff, val cloudBackup: CloudBackup)

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        CloudBackupDiffAdapter()
    }

    init {
        setContentView(R.layout.fragment_backup_diff)
        binder.backupDiffSubtitle.text = buildSubtitleText()
        binder.backupDiffList.adapter = adapter

        binder.backupDiffCancel.setOnClickListener { dismiss() }
        binder.backupDiffApply.setOnClickListener {
            onApply(payload.cloudBackupDiff, payload.cloudBackup)
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
