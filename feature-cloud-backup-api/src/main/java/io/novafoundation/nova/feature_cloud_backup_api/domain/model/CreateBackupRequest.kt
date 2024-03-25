package io.novafoundation.nova.feature_cloud_backup_api.domain.model

import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic

class CreateBackupRequest(
    val mnemonic: Mnemonic,
    val walletName: String,
    val password: String
)

