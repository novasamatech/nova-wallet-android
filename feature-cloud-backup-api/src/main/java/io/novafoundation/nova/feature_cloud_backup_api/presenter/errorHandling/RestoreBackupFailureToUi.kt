package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CorruptedBackupError
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.InvalidBackupPasswordError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupInvalidPassword
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupNotFound
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupUnknownError

fun mapRestoreBackupFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable,
    corruptedBackupFound: () -> Unit
): TitleAndMessage? {
    return when (throwable) {
        is InvalidBackupPasswordError -> handleCloudBackupInvalidPassword(resourceManager)

        is CorruptedBackupError -> {
            corruptedBackupFound()
            return null
        }

        is CloudBackupNotFound -> handleCloudBackupNotFound(resourceManager)

        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}

fun mapDeleteBackupFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage? {
    return when (throwable) {
        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}

fun mapCheckPasswordFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage? {
    return when (throwable) {
        is InvalidBackupPasswordError -> handleCloudBackupInvalidPassword(resourceManager)

        else -> null
    }
}

fun mapRestorePasswordFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable,
    corruptedBackupFound: () -> Unit
): TitleAndMessage? {
    return when (throwable) {
        is InvalidBackupPasswordError -> handleCloudBackupInvalidPassword(resourceManager)

        is CorruptedBackupError -> {
            corruptedBackupFound()
            null
        }

        is CloudBackupNotFound -> handleCloudBackupNotFound(resourceManager)

        else -> handleCloudBackupUnknownError(resourceManager)
    }
}
