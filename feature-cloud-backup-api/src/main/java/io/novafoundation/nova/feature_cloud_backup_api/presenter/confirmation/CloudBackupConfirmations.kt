package io.novafoundation.nova.feature_cloud_backup_api.presenter.confirmation

import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationAwaitable
import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmationDialogInfo
import io.novafoundation.nova.feature_cloud_backup_api.R

suspend fun ConfirmationAwaitable<ConfirmationDialogInfo>.awaitDeleteBackupConfirmation() {
    awaitAction(
        ConfirmationDialogInfo(
            title = R.string.cloud_backup_delete_backup_confirmation_title,
            message = R.string.cloud_backup_delete_backup_confirmation_message,
            positiveButton = R.string.cloud_backup_delete_button,
            negativeButton = R.string.common_cancel
        )
    )
}
