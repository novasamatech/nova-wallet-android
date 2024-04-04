package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

sealed class FetchBackupError : Throwable() {

    object BackupNotFound : FetchBackupError(), CloudBackupNotFound

    object Other : FetchBackupError(), CloudBackupUnknownError
}
