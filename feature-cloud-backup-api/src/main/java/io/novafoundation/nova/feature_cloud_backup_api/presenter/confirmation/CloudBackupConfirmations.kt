package io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation

import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.feature_cloud_backup_api.R

suspend fun ConfirmationAwaitable<ConfirmationDialogInfo>.awaitDeleteBackupConfirmation() {
    awaitAction(
        ConfirmationDialogInfo.ByRes(
            title = R.string.cloud_backup_delete_backup_confirmation_title,
            message = R.string.cloud_backup_delete_backup_confirmation_message,
            positiveButton = R.string.cloud_backup_delete_button,
            negativeButton = R.string.common_cancel
        )
    )
}

suspend fun ConfirmationAwaitable<ConfirmationDialogInfo>.awaitBackupDestructiveChangesConfirmation() {
    awaitAction(
        ConfirmationDialogInfo.ByRes(
            title = R.string.cloud_backup_destructive_changes_confirmation_title,
            message = R.string.cloud_backup_destructive_changes_confirmation_subtitle,
            positiveButton = R.string.common_apply,
            negativeButton = R.string.common_cancel
        )
    )
}
