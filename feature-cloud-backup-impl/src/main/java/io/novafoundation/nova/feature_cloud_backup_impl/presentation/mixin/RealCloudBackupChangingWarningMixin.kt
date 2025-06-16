package io.novafoundation.nova.feature_cloud_backup_impl.presentation.mixin

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.CheckBoxPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.negative
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.common.view.bottomSheet.action.secondary
import io.novafoundation.nova.feature_cloud_backup_api.R
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixin
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val KEY_CLOUD_BACKUP_CHANGE_WARNING_SHOWN = "cloud_backup_change_warning_shown"

class RealCloudBackupChangingWarningMixinFactory(
    private val preferences: Preferences,
    private val resourceManager: ResourceManager,
    private val cloudBackupService: CloudBackupService,
    private val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory
) : CloudBackupChangingWarningMixinFactory {

    override fun create(coroutineScope: CoroutineScope): CloudBackupChangingWarningMixin {
        return RealCloudBackupChangingWarningMixin(
            coroutineScope,
            preferences,
            resourceManager,
            cloudBackupService,
            actionBottomSheetLauncherFactory
        )
    }
}

class RealCloudBackupChangingWarningMixin(
    private val scope: CoroutineScope,
    private val preferences: Preferences,
    private val resourceManager: ResourceManager,
    private val cloudBackupService: CloudBackupService,
    actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory
) : CloudBackupChangingWarningMixin {

    override val actionBottomSheetLauncher: ActionBottomSheetLauncher = actionBottomSheetLauncherFactory.create()

    override fun launchChangingConfirmationIfNeeded(onConfirm: () -> Unit) {
        scope.launch {
            // In case if cloud backup sync is disabled, we don't need to show the warning and can confirm the action now
            if (!cloudBackupService.session.isSyncWithCloudEnabled()) {
                onConfirm()
                return@launch
            }

            if (preferences.getBoolean(KEY_CLOUD_BACKUP_CHANGE_WARNING_SHOWN, false)) {
                onConfirm()
                return@launch
            }

            actionBottomSheetLauncher.launchCloudBackupChangingWarning(resourceManager, onConfirm)
        }
    }

    override fun launchRemovingConfirmationIfNeeded(onConfirm: () -> Unit) {
        scope.launch {
            // In case if cloud backup sync is disabled, we don't need to show the warning and can confirm the action now
            if (!cloudBackupService.session.isSyncWithCloudEnabled()) {
                onConfirm()
                return@launch
            }

            actionBottomSheetLauncher.launchCloudBackupRemovingWarning(resourceManager, onConfirm)
        }
    }

    private fun ActionBottomSheetLauncher.launchCloudBackupChangingWarning(
        resourceManager: ResourceManager,
        onConfirm: () -> Unit
    ) {
        var isAutoContinueChecked = false

        launchBottomSheet(
            imageRes = R.drawable.ic_cloud_backup_add,
            title = resourceManager.getString(R.string.cloud_backup_will_be_changed_title),
            subtitle = resourceManager.getString(R.string.cloud_backup_will_be_changed_subtitle),
            neutralButtonPreferences = ButtonPreferences.secondary(
                resourceManager.getString(
                    R.string.common_cancel
                )
            ),
            actionButtonPreferences = ButtonPreferences.primary(
                resourceManager.getString(R.string.common_continue),
                onClick = {
                    preferences.putBoolean(KEY_CLOUD_BACKUP_CHANGE_WARNING_SHOWN, isAutoContinueChecked)
                    onConfirm()
                }
            ),
            checkBoxPreferences = CheckBoxPreferences(
                text = resourceManager.getString(R.string.common_check_box_auto_continue),
                onCheckChanged = { isChecked -> isAutoContinueChecked = isChecked }
            )
        )
    }

    private fun ActionBottomSheetLauncher.launchCloudBackupRemovingWarning(
        resourceManager: ResourceManager,
        onConfirm: () -> Unit
    ) {
        launchBottomSheet(
            imageRes = R.drawable.ic_cloud_backup_delete,
            title = resourceManager.getString(R.string.cloud_backup_removing_warning_title),
            subtitle = resourceManager.getString(R.string.cloud_backup_removing_warning_subtitle),
            neutralButtonPreferences = ButtonPreferences.secondary(
                resourceManager.getString(
                    R.string.common_cancel
                )
            ),
            actionButtonPreferences = ButtonPreferences.negative(
                resourceManager.getString(R.string.common_remove),
                onClick = onConfirm
            )
        )
    }
}
