package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

sealed class WriteBackupError : Throwable() {

    object Other : WriteBackupError()
}
