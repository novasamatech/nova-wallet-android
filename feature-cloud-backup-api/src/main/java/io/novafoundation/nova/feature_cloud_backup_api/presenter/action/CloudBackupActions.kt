package io.novafoundation.nova.feature_cloud_backup_api.presenter.action

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.spannableFormatting
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.negative
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.common.view.bottomSheet.action.secondary
import io.novafoundation.nova.feature_cloud_backup_api.R

fun ActionBottomSheetLauncher.launchBackupLostPasswordAction(resourceManager: ResourceManager, onDeleteClicked: () -> Unit) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_password,
        title = resourceManager.getString(R.string.restore_cloud_backup_delete_backup_title),
        subtitle = with(resourceManager) {
            val highlightedFirstPart = getString(R.string.restore_cloud_backup_delete_backup_description_highlighted_1)
                .addColor(getColor(R.color.text_primary))

            val highlightedSecondPart = getString(R.string.restore_cloud_backup_delete_backup_description_highlighted_2)
                .addColor(getColor(R.color.text_primary))

            getString(R.string.restore_cloud_backup_delete_backup_description).spannableFormatting(highlightedFirstPart, highlightedSecondPart)
        },
        neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(
            resourceManager.getString(
                R.string.common_cancel
            )
        ),
        actionButtonPreferences = ActionBottomSheet.ButtonPreferences.negative(
            resourceManager.getString(R.string.cloud_backup_delete_button),
            onDeleteClicked
        )
    )
}

fun ActionBottomSheetLauncher.launchDeleteBackupAction(resourceManager: ResourceManager, onDeleteClicked: () -> Unit) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_delete,
        title = resourceManager.getString(R.string.cloud_backup_delete_action_title),
        subtitle = with(resourceManager) {
            val highlightedPart = getString(R.string.cloud_backup_delete_action_subtitle_highlighted)
                .addColor(getColor(R.color.text_primary))
            getString(R.string.cloud_backup_delete_action_subtitle).spannableFormatting(highlightedPart)
        },
        neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(
            resourceManager.getString(
                R.string.common_cancel
            )
        ),
        actionButtonPreferences = ActionBottomSheet.ButtonPreferences.negative(
            resourceManager.getString(R.string.cloud_backup_delete_button),
            onDeleteClicked
        )
    )
}

fun ActionBottomSheetLauncher.launchRememberPasswordWarning(resourceManager: ResourceManager) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_lock,
        title = resourceManager.getString(R.string.create_cloud_backup_password_alert_title),
        subtitle = with(resourceManager) {
            val highlightedPart = getString(R.string.create_cloud_backup_password_alert_subtitle_highlighted)
                .addColor(getColor(R.color.text_primary))

            getString(R.string.create_cloud_backup_password_alert_subtitle).spannableFormatting(highlightedPart)
        },
        actionButtonPreferences = ActionBottomSheet.ButtonPreferences.primary(resourceManager.getString(R.string.common_got_it))
    )
}

fun ActionBottomSheetLauncher.launchCorruptedBackupFoundAction(resourceManager: ResourceManager, onDeleteClicked: () -> Unit) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_error,
        title = resourceManager.getString(R.string.corrupted_backup_error_title),
        subtitle = with(resourceManager) {
            val highlightedPart = getString(R.string.corrupted_backup_error_subtitle_highlighted)
                .addColor(getColor(R.color.text_primary))

            getString(R.string.corrupted_backup_error_subtitle).spannableFormatting(highlightedPart)
        },
        neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
        actionButtonPreferences = ActionBottomSheet.ButtonPreferences.negative(
            resourceManager.getString(R.string.cloud_backup_delete_button),
            onDeleteClicked
        )
    )
}

fun ActionBottomSheetLauncher.launchExistingCloudBackupAction(resourceManager: ResourceManager, onImportClicked: () -> Unit) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_sync,
        title = resourceManager.getString(R.string.existing_cloud_backup_found_title),
        subtitle = with(resourceManager) {
            val highlightedPart = getString(R.string.existing_cloud_backup_found_subtitle_highlight)
                .addColor(getColor(R.color.text_primary))

            getString(R.string.existing_cloud_backup_found_subtitle).spannableFormatting(highlightedPart)
        },
        neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
        actionButtonPreferences = ActionBottomSheet.ButtonPreferences.primary(
            resourceManager.getString(R.string.existing_cloud_backup_found_button),
            onImportClicked
        )
    )
}
