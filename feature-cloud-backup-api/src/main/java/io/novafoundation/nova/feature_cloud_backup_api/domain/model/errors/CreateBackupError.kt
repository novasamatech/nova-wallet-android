package io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors

sealed class CreateBackupError : Throwable() {

    object NotEnoughSpace : CreateBackupError()

    object Other : CreateBackupError()
}
