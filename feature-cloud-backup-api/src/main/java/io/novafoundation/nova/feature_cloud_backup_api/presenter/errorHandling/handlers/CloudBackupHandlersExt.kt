package io.novafoundation.nova.feature_cloud_backup_api.presenter.errorHandling.handlers

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.showError
import io.novafoundation.nova.common.resources.ResourceManager

fun BaseViewModel.showCloudBackupUnknownError(resourceManager: ResourceManager) {
    showError(handleCloudBackupUnknownError(resourceManager))
}
