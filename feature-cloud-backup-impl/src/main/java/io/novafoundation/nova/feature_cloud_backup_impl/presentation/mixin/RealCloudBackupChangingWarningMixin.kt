package io.novafoundation.nova.feature_cloud_backup_impl.presentation.mixin

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncher
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.action.primary
import io.novafoundation.nova.common.view.bottomSheet.action.secondary
import io.novafoundation.nova.feature_cloud_backup_api.R
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixin
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val KEY_CLOUD_BACKUP_WARNING_SHOWN = "cloud_backup_warning_shown"

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

    override fun launchConfirmationIfNeeded(onConfirm: () -> Unit) {
        scope.launch {
            // In case if cloud backup sync is disabled, we don't need to show the warning and can confirm the action now
            if (!cloudBackupService.session.isSyncWithCloudEnabled()) {
                onConfirm()
                return@launch
            }

            if (preferences.getBoolean(KEY_CLOUD_BACKUP_WARNING_SHOWN, false)) {
                onConfirm()
                return@launch
            }

            actionBottomSheetLauncher.launchCloudBackupWillBeChangedWarning(resourceManager, onConfirm)
        }
    }

    fun ActionBottomSheetLauncher.launchCloudBackupWillBeChangedWarning(
        resourceManager: ResourceManager,
        onConfirm: () -> Unit
    ) {
        var isAutoContinueChecked = false

        launchBottomSheet(
            imageRes = R.drawable.ic_cloud_backup_sync,
            title = resourceManager.getString(R.string.cloud_backup_will_be_changed_title),
            subtitle = resourceManager.getString(R.string.cloud_backup_will_be_changed_subtitle),
            neutralButtonPreferences = ActionBottomSheet.ButtonPreferences.secondary(resourceManager.getString(R.string.common_cancel)),
            actionButtonPreferences = ActionBottomSheet.ButtonPreferences.primary(
                resourceManager.getString(R.string.common_continue),
                onClick = {
                    preferences.putBoolean(KEY_CLOUD_BACKUP_WARNING_SHOWN, isAutoContinueChecked)
                    onConfirm()
                }),
            checkBoxPreferences = ActionBottomSheet.CheckBoxPreferences(
                text = resourceManager.getString(R.string.common_check_box_auto_continue),
                onCheckChanged = { isChecked -> isAutoContinueChecked = isChecked }
            )
        )
    }
}
