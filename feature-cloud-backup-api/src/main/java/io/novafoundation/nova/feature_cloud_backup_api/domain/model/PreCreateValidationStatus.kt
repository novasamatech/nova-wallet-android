package io.novafoundation.nova.feature_cloud_backup_api.domain.model

sealed class PreCreateValidationStatus {

    object Ok : PreCreateValidationStatus()

    object AuthenticationFailed : PreCreateValidationStatus()

    object BackupServiceUnavailable : PreCreateValidationStatus()

    object ExistingBackupFound : PreCreateValidationStatus()

    object NotEnoughSpace : PreCreateValidationStatus()

    object OtherError : PreCreateValidationStatus()
}
