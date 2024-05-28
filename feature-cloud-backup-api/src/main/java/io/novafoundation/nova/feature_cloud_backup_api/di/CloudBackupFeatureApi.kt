package io.novafoundation.nova.feature_cloud_backup_api.di

import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory

interface CloudBackupFeatureApi {

    val cloudBackupService: CloudBackupService

    val cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
}
