package io.novafoundation.nova.feature_account_api.data.cloudBackup

import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_cloud_backup_api.domain.model.CloudBackup

fun CloudBackup.WalletPublicInfo.Type.toMetaAccountType(): LightMetaAccount.Type {
    return when (this) {
        CloudBackup.WalletPublicInfo.Type.SECRETS -> LightMetaAccount.Type.SECRETS
        CloudBackup.WalletPublicInfo.Type.WATCH_ONLY -> LightMetaAccount.Type.WATCH_ONLY
        CloudBackup.WalletPublicInfo.Type.PARITY_SIGNER -> LightMetaAccount.Type.PARITY_SIGNER
        CloudBackup.WalletPublicInfo.Type.LEDGER -> LightMetaAccount.Type.LEDGER
        CloudBackup.WalletPublicInfo.Type.POLKADOT_VAULT -> LightMetaAccount.Type.POLKADOT_VAULT
    }
}
