package io.novafoundation.nova.feature_ledger_api.sdk.application.substrate

import io.novasama.substrate_sdk_android.encrypt.EncryptionType

class LedgerSubstrateAccount(
    val address: String,
    val publicKey: ByteArray,
    val encryptionType: EncryptionType,
    val derivationPath: String,
)
