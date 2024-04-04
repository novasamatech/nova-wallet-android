package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling

import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_cloud_backup_api.R

fun handleCloudBackupAuthFailed(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.cloud_backup_error_google_service_auth_failed_title),
        resourceManager.getString(R.string.cloud_backup_error_google_service_auth_failed_message)
    )
}

fun handleCloudBackupServiceUnavailable(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.cloud_backup_error_google_service_not_found_title),
        resourceManager.getString(R.string.cloud_backup_error_google_service_not_found_message)
    )
}

fun handleCloudBackupNotEnoughSpace(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.cloud_backup_error_google_service_not_enough_space_title),
        resourceManager.getString(R.string.cloud_backup_error_google_service_not_enough_space_message)
    )
}

fun handleCloudBackupConnectionError(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.common_connection_error_title),
        resourceManager.getString(R.string.common_connection_error_message)
    )
}

fun handleCloudBackupNotFound(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.cloud_backup_error_not_found_title),
        resourceManager.getString(R.string.cloud_backup_error_not_found_message)
    )
}

fun handleCloudBackupUnknownError(resourceManager: ResourceManager): TitleAndMessage {
    return TitleAndMessage(
        resourceManager.getString(R.string.cloud_backup_error_google_service_other_title),
        resourceManager.getString(R.string.cloud_backup_error_google_service_other_message)
    )
}
