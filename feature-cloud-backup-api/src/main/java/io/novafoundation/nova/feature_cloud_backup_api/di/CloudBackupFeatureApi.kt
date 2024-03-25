package io.novafoundation.nova.feature_cloud_backup_api.di

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService

interface CloudBackupFeatureApi {

    val cloudBackupService: CloudBackupService
}
