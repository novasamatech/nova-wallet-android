package io.novafoundation.nova.feature_cloud_backup_api.domain.model

class WriteBackupRequest(
    val cloudBackup: CloudBackup,
    val password: String
)
