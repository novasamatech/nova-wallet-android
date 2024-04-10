package io.novafoundation.nova.feature_cloud_backup_impl.presentation.settings.main

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_cloud_backup_impl.domain.settings.CloudBackupSettingsInteractor
import io.novafoundation.nova.feature_cloud_backup_impl.presentation.CloudBackupRouter
import kotlinx.coroutines.launch

class CloudBackupSettingsViewModel(
    private val router: CloudBackupRouter,
    private val cloudBackupSettingsInteractor: CloudBackupSettingsInteractor
) : BaseViewModel() {

    val cloudBackupEnabled = flowOf { false }

    fun backClicked() {
        router.back()
    }

    fun backupSwitcherClicked() {
        launch {
            cloudBackupSettingsInteractor.toggleCloudBackupState()
        }
    }

    fun manualBackupClicked() {
        TODO()
    }
}
