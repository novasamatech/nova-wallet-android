package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

sealed class DeleteBackupError : Throwable() {

    object Other : DeleteBackupError(), CloudBackupUnknownError
}
