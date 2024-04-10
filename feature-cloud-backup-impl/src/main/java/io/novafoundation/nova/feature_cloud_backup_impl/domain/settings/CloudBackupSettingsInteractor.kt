package io.novafoundation.nova.feature_cloud_backup_impl.domain.settings

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService

interface CloudBackupSettingsInteractor {
    suspend fun toggleCloudBackupState()
}

class RealCloudBackupSettingsInteractor(
    private val cloudBackupService: CloudBackupService
) : CloudBackupSettingsInteractor {

    override suspend fun toggleCloudBackupState() {
        TODO()
    }
}
