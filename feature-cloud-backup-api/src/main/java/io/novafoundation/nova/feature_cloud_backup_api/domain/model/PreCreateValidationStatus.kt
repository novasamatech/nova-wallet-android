package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupConnectionError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupExistingBackupFound
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotEnoughSpace
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupServiceUnavailable
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError

sealed class PreCreateValidationStatus {

    object Ok : PreCreateValidationStatus()

    object AuthenticationFailed : PreCreateValidationStatus(), CloudBackupAuthFailed

    object BackupServiceUnavailable : PreCreateValidationStatus(), CloudBackupServiceUnavailable

    object ExistingBackupFound : PreCreateValidationStatus(), CloudBackupExistingBackupFound

    object NotEnoughSpace : PreCreateValidationStatus(), CloudBackupNotEnoughSpace

    object ConnectionError : PreCreateValidationStatus(), CloudBackupConnectionError

    object OtherError : PreCreateValidationStatus(), CloudBackupUnknownError
}
