package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors


sealed class FetchBackupError : Throwable() {

    object ConnectionFailure : FetchBackupError()

    object BackupNotFound: FetchBackupError()

    object Other : FetchBackupError()
}
