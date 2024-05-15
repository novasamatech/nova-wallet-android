package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.mixin.api.toCustomDialogPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupServiceUnavailable
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupAuthFailed
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupServiceUnavailable
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers.handleCloudBackupUnknownError

fun mapCheckBackupAvailableFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable,
    initSignIn: () -> Unit
): CustomDialogDisplayer.Payload {
    return when (throwable) {
        is CloudBackupAuthFailed -> handleCloudBackupAuthFailed(resourceManager, initSignIn)

        is CloudBackupServiceUnavailable -> handleCloudBackupServiceUnavailable(resourceManager).toCustomDialogPayload(resourceManager)

        else -> handleCloudBackupUnknownError(resourceManager).toCustomDialogPayload(resourceManager)
    }
}
