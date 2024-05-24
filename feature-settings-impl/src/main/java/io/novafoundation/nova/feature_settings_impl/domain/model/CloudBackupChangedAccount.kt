package io.novafoundation.nova.feature_settings_impl.domain.model

import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup

class CloudBackupChangedAccount(val changingType: ChangingType, val account: CloudBackup.WalletPublicInfo) {

    enum class ChangingType {
        ADDED,
        REMOVED,
        CHANGED
    }
}
