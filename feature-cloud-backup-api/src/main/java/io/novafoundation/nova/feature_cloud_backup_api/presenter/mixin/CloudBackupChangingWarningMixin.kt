package io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin

import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import kotlinx.coroutines.CoroutineScope

interface CloudBackupChangingWarningMixinFactory {

    fun create(coroutineScope: CoroutineScope): CloudBackupChangingWarningMixin
}

interface CloudBackupChangingWarningMixin {

    val actionBottomSheetLauncher: ActionBottomSheetLauncher

    fun launchChangingConfirmationIfNeeded(onConfirm: () -> Unit)

    fun launchRemovingConfirmationIfNeeded(onConfirm: () -> Unit)
}
