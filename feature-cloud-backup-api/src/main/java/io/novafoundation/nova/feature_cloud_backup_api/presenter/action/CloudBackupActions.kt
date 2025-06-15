package io.novafoundation.nova.feature_cloud_backup_api.presenter.action

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.addColor
import io.novafoundation.nova.common.utils.formatting.spannable.spannableFormatting
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
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
        neutralButtonPreferences = ButtonPreferences.secondary(
            resourceManager.getString(
                R.string.common_cancel
            )
        ),
        actionButtonPreferences = ButtonPreferences.negative(
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
        neutralButtonPreferences = ButtonPreferences.secondary(
            resourceManager.getString(
                R.string.common_cancel
            )
        ),
        actionButtonPreferences = ButtonPreferences.negative(
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
        actionButtonPreferences = ButtonPreferences.primary(resourceManager.getString(R.string.common_got_it))
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
        neutralButtonPreferences = ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
        actionButtonPreferences = ButtonPreferences.negative(
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
        neutralButtonPreferences = ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
        actionButtonPreferences = ButtonPreferences.primary(
            resourceManager.getString(R.string.existing_cloud_backup_found_button),
            onImportClicked
        )
    )
}

fun ActionBottomSheetLauncher.launchDeprecatedPasswordAction(resourceManager: ResourceManager, onEnterPasswordClick: () -> Unit) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_password,
        title = resourceManager.getString(R.string.deprecated_cloud_backup_password_title),
        subtitle = with(resourceManager) {
            val highlightedPart = getString(R.string.deprecated_cloud_backup_password_subtitle_highlight)
                .addColor(getColor(R.color.text_primary))

            getString(R.string.deprecated_cloud_backup_password_subtitle).spannableFormatting(highlightedPart)
        },
        neutralButtonPreferences = ButtonPreferences.secondary(resourceManager.getString(R.string.common_not_now)),
        actionButtonPreferences = ButtonPreferences.primary(
            resourceManager.getString(R.string.common_enter_password),
            onEnterPasswordClick
        )
    )
}

fun ActionBottomSheetLauncher.launchCloudBackupChangesAction(resourceManager: ResourceManager, onReviewClicked: () -> Unit) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_warning,
        title = resourceManager.getString(R.string.cloud_backup_destructive_changes_action_title),
        subtitle = resourceManager.getString(R.string.cloud_backup_destructive_changes_action_subtitle),
        neutralButtonPreferences = ButtonPreferences.secondary(
            resourceManager.getString(R.string.common_not_now)
        ),
        actionButtonPreferences = ButtonPreferences.primary(
            resourceManager.getString(R.string.cloud_backup_destructive_changes_button),
            onReviewClicked
        )
    )
}

fun ActionBottomSheetLauncher.launchCloudBackupDestructiveChangesNotApplied(
    resourceManager: ResourceManager,
    onReviewClicked: () -> Unit
) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_warning,
        title = resourceManager.getString(R.string.cloud_backup_destructive_changes_not_applied_title),
        subtitle = resourceManager.getString(R.string.cloud_backup_destructive_changes_not_applied_subtitle),
        neutralButtonPreferences = ButtonPreferences.secondary(
            resourceManager.getString(R.string.common_not_now)
        ),
        actionButtonPreferences = ButtonPreferences.negative(
            resourceManager.getString(R.string.cloud_backup_destructive_changes_not_applied_button),
            onReviewClicked
        )
    )
}

fun ActionBottomSheetLauncher.launchCloudBackupDestructiveChangesNotAppliedWithoutRouting(
    resourceManager: ResourceManager
) {
    launchBottomSheet(
        imageRes = R.drawable.ic_cloud_backup_warning,
        title = resourceManager.getString(R.string.cloud_backup_destructive_changes_not_applied_title),
        subtitle = resourceManager.getString(R.string.cloud_backup_destructive_changes_not_applied_subtitle),
        actionButtonPreferences = ButtonPreferences.secondary(
            resourceManager.getString(R.string.common_got_it)
        )
    )
}
