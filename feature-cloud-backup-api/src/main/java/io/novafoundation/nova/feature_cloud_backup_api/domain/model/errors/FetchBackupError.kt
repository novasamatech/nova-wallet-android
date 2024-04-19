package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

sealed class FetchBackupError : Throwable() {

    object AuthFailed : FetchBackupError(), CloudBackupAuthFailed

    object BackupNotFound : FetchBackupError(), CloudBackupNotFound

    object CorruptedBackup : FetchBackupError(), CorruptedBackupError

    object Other : FetchBackupError(), CloudBackupUnknownError
}
