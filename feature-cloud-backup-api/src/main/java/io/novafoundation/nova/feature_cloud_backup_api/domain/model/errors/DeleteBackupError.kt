package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

sealed class DeleteBackupError {

    object ConnectionFailure : DeleteBackupError()

    object Other : DeleteBackupError()
}
