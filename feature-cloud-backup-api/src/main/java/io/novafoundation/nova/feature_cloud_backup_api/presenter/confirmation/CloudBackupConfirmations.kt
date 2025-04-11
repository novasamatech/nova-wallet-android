package io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation

import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.common.mixin.actionAwaitable.fromRes
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.R

suspend fun ConfirmationAwaitable<ConfirmationDialogInfo>.awaitDeleteBackupConfirmation(resourceManager: ResourceManager) {
    awaitAction(
        ConfirmationDialogInfo.fromRes(
            resourceManager,
            title = R.string.cloud_backup_delete_backup_confirmation_title,
            message = R.string.cloud_backup_delete_backup_confirmation_message,
            positiveButton = R.string.cloud_backup_delete_button,
            negativeButton = R.string.common_cancel
        )
    )
}

suspend fun ConfirmationAwaitable<ConfirmationDialogInfo>.awaitBackupDestructiveChangesConfirmation(resourceManager: ResourceManager) {
    awaitAction(
        ConfirmationDialogInfo.fromRes(
            resourceManager,
            title = R.string.cloud_backup_destructive_changes_confirmation_title,
            message = R.string.cloud_backup_destructive_changes_confirmation_subtitle,
            positiveButton = R.string.common_apply,
            negativeButton = R.string.common_cancel
        )
    )
}
