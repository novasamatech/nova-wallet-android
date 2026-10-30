package io.novafoundation.nova.feature_settings_impl.presentation.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import io.novafoundation.nova.common.view.bottomSheet.BaseBottomSheet
import io.novafoundation.nova.feature_settings_impl.databinding.BottomSheetDebugSettingsBinding

class DebugSettingsBottomSheet(
    context: Context,
    private val claimEnabled: Boolean,
    private val onClaimChanged: (Boolean) -> Unit,
    private val claimButtonEnabled: Boolean,
    private val onClaimButtonChanged: (Boolean) -> Unit,
    private val nodeLogsDisabled: Boolean,
    private val onNodeLogsChanged: (Boolean) -> Unit
) : BaseBottomSheet<BottomSheetDebugSettingsBinding>(context) {

    override val binder = BottomSheetDebugSettingsBinding.inflate(LayoutInflater.from(context))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binder.disableNodeLogs.field.isChecked = nodeLogsDisabled
        binder.disableNodeLogs.field.setOnCheckedChangeListener { _, disabled -> onNodeLogsChanged(disabled) }
        binder.claimDebug.field.isChecked = claimEnabled
        binder.claimButtonDebug.field.isChecked = claimButtonEnabled
        binder.claimDebug.field.setOnCheckedChangeListener { _, enabled ->
            onClaimChanged(enabled)
            if (enabled) binder.claimButtonDebug.field.isChecked = true
        }
        binder.claimButtonDebug.field.setOnCheckedChangeListener { _, enabled ->
            onClaimButtonChanged(enabled)
            if (!enabled) binder.claimDebug.field.isChecked = false
        }
    }
}
