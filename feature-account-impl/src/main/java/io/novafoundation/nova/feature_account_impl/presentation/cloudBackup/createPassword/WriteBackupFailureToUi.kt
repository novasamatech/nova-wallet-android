package io.novafoundation.nova.feature_account_impl.presentation.cloudBackup.createPassword

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.errors.CloudBackupUnknownError
import io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handleCloudBackupUnknownError

fun mapWriteBackupFailureToUi(
    resourceManager: ResourceManager,
    throwable: Throwable
): TitleAndMessage? {
    return when (throwable) {
        is CloudBackupUnknownError -> handleCloudBackupUnknownError(resourceManager)

        else -> null
    }
}
