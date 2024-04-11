package io.novafoundation.nova.feature_cloud_backup_api.domain.model.diff

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup

class SourcedBackupChanges<SOURCE>(val changes: List<CloudBackup.WalletPublicInfo>) {

    object LocalWallets

    object WalletsFromCloud
}
